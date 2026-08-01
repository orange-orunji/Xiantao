# 知味点评 · 面试速通考点手册

> 整理日期：2026-08-01
> 适用场景：面试前 1 小时快速过一遍考点，配合 `interview-guide.md`（详细话术）使用
> 定位：只列考点 + 一句话答案 + 追问方向，背熟即可开口讲

---

## 〇、30 秒项目定位（开口第一句）

> 知味是一个**本地生活服务点评平台**（类似大众点评），核心功能：附近商户 GEO 搜索、高并发**优惠券秒杀**、博客社区（点赞/Feed流）、用户关注/私信/通知。
>
> 我独立完成后端架构设计与核心实现。基于 **Spring Boot 2.7 + MyBatis-Plus + Redis + RabbitMQ**，Docker Compose 一键部署。
>
> 秒杀接口经 9 轮压测调优，错误率从 **100% 降到 7.89%**，所有到达服务端的请求全部处理成功。

---

## 一、技术栈速查（记版本号）

| 技术 | 版本 | 考点一句话 |
|:---|:---|:---|
| Spring Boot | 2.7.18 | 内嵌 Tomcat，线程池 max=500 |
| JDK | 17 | — |
| MyBatis-Plus | 3.4.3 | 条件构造器 + 代码生成提效 |
| MySQL | 8.0.30 | 订单/用户数据落库 |
| Redis | 7.x + Lettuce 6.8.2 | 缓存/GEO/分布式锁/Lua/BitMap/ZSet |
| Redisson | 3.16.2 | 封装 Redis 分布式锁 |
| RabbitMQ | 3.8+ | 秒杀异步削峰 + 死信兜底 |
| Nginx | 1.18.0 | 反向代理 `/api/` + 静态资源 |
| Knife4j | 4.3.0 | Swagger 增强 API 文档（`/doc.html`） |
| Hutool | 5.7.17 | Bean/JSON 工具 |

---

## 二、六大核心考点（必背）

### 考点 1：登录认证 — 双拦截器 + Redis 存 Token

**实现链路**：手机号 → 校验 → 发验证码（Redis 存 2 分钟）→ 登录成功生成 UUID Token → 用户信息存 **Redis Hash**（`login:token:{uuid}`，TTL 36000 分钟）→ 请求头带 Token 校验。

**双拦截器**（`WebMvcConfig`）：
- `ReflashTokenInterceptor`（order=0）：有 Token 就查 Redis，存在则**刷新 TTL**，并存入 ThreadLocal
- `LoginInterceptor`（order=1）：校验登录态，未登录**放行**（由业务层判断），避免拦截全部接口
- `UserHolder`（ThreadLocal）传递用户上下文，`afterCompletion` 必须清除（防内存泄漏）

**追问方向**：
- Q：为什么不用 Tomcat Session？→ 集群部署需 Session 共享，Redis 中心化天然支持
- Q：Token 有效期怎么续？→ 每次请求拦截器里 `expire` 重置 TTL（滑动续期）
- Q：ThreadLocal 为什么必须 remove？→ Tomcat 线程池复用线程，不清理会串用户数据

---

### 考点 2：缓存三大问题 + 缓存架构演进

| 问题 | 现象 | 解决方案（本项目） |
|:---|:---|:---|
| **穿透** | 查不存在的数据，打到 DB | 缓存**空对象**（TTL=2 分钟）；备选布隆过滤器 |
| **击穿** | 热点 key 过期瞬间打爆 DB | ①**互斥锁**（SETNX 抢锁回源）②**逻辑过期**（数据永不过期，异步线程重建） |
| **雪崩** | 大量 key 同时过期 | 过期时间加随机值 / 多级缓存 / 限流降级 |

**架构演进 4 层**：
1. 基础缓存：查 Redis → miss → 查 DB → 写回
2. 穿透防护：空对象缓存
3. 击穿防护：互斥锁 + 逻辑过期两种方案
4. **通用 CacheClient 工具类**：泛型 + `Function` 函数式接口封装
   - `queryWithExpireTime()`：逻辑过期方案（生产默认）
   - `queryWithMiddleLock()`：互斥锁方案
   - `setWithLogicalExpire()`：写逻辑过期数据（`RedisData` 包装 + expireTime）

**细节记忆**：
- 逻辑过期实现：`RedisData{data, expireTime}`，过期后抢锁成功则丢线程池（10 线程）异步回源，**直接返回旧数据**（牺牲一致性换可用性）
- 互斥锁 TTL=10s，自旋 50ms 重试；解锁用 Lua 判断标识防误删

**追问方向**：
- Q：逻辑过期 vs 互斥锁怎么选？→ 逻辑过期**性能高但返回旧数据**，适合热点；互斥锁**数据一致但会阻塞**，适合一致性要求高
- Q：先更新 DB 还是先删缓存？→ 更新 DB 后删缓存（Cache Aside），配合延迟双删
- Q：空对象缓存会不会导致大量 key 堆积？→ 短 TTL + 定期清理

---

### 考点 3：分布式 ID — RedisIdWorker

**结构**：64 位 Long = 高 32 位（秒级时间戳 - 基准时间戳）+ 低 32 位（Redis INCR 自增序列号）

```java
long second = now.toEpochSecond(ZoneOffset.UTC) - BEGIN_TIMESTAMP;
Long seq = stringRedisTemplate.opsForValue().increment("icr" + prefix + date);
return second << 32 | seq;
```

**关键点**：
- 序列号 key 带**日期后缀**（`icr:order:260801`）→ 每天独立计数，避免 32 位溢出
- 一天最多 2^32 个 ID ≈ 42 亿，足够
- 时间戳部分全局有序 → **趋势递增**，对 MySQL 聚簇索引友好

**追问方向**：为什么不用雪花算法？→ 雪花依赖机器时间，时钟回拨会出问题；Redis 方案更简单且可控

---

### 考点 4：分布式锁（演进过程）

**阶段一：自研 Redis 锁（`IRedisLock`）**
- 加锁：`SET lock:xx {uuid}-{threadId} NX EX 10`（保证原子 + 过期防死锁）
- 解锁：**Lua 脚本**先 GET 比较 value 再 DEL（防止误删别人的锁）

**阶段二：Redisson（`RLock`）**
- `tryLock(waitTime, leaseTime)` 支持**看门狗自动续期**（默认 30s，业务没结束自动续期）
- 解决自研锁的锁过期但业务未完成的问题

**追问方向**：
- Q：为什么解锁要用 Lua？→ 判断 + 删除两步非原子，极端情况下会删掉别人的锁
- Q：锁过期了业务没执行完怎么办？→ Redisson 看门狗机制自动续期
- Q：Redis 主从复制下锁丢失？→ RedLock 红锁（多实例投票），但很少用

---

### 考点 5：秒杀全链路（项目最大亮点，必须画得出）

```
用户点秒杀 → ① 滑动窗口限流(Lua) → ② Lua 原子扣库存/查重 → ③ RabbitMQ 发消息
    → ④ 消费者异步下单(幂等+乐观锁) → ⑤ 失败进死信队列 → 补偿 Redis 库存
```

**第 1 关 — 滑动窗口限流（`rate_limit.lua`）**：
- 每用户每秒最多 5 次，ZSet 存请求时间戳
- `ZREMRANGEBYSCORE` 清过期 → `ZCARD` 判断 → `ZADD` 记录
- Lua 保证原子性；单机 Tomcat 限流只能 JVM 内，Redis 是**分布式限流**

**第 2 关 — Lua 原子秒杀（`seckill.lua`）**：
- 参数：voucherId / userId / orderId
- 逻辑：`GET 库存 → 判空 → SISMEMBER 判重复 → SET 扣减 → SADD 记录用户`，全部在一个 Lua 脚本内原子执行
- 返回值：1=库存不足，2=重复下单，0=成功
- **注意**：扣库存用 `SET stock-1` 而非 `INCRBY`（INCRBY 遇脏数据会崩溃，压测踩过坑）

**第 3 关 — RabbitMQ 异步下单**：
- 秒杀成功立即返回 orderId，不等待落库
- 消息发到 `order.exchange`（DIRECT）→ 路由 `order.generate` → `order.queue`
- 生产者确认：`ConfirmCallback`（到 Exchange 失败）+ `ReturnsCallback`（路由不到队列），失败记录 Redis Set `order:fail` 人工补偿

**第 4 关 — 消费者（`OrderConsumer`）**：
- 手动 ACK（`acknowledge-mode: manual`），成功 `basicAck`，失败 `basicNack(requeue=false)` 进死信
- 幂等校验：先查 DB 是否已有该用户+券订单
- 乐观锁更新库存：`stock = stock - 1 WHERE stock > 0`
- 消费并发 concurrency=20~50，prefetch=50

**第 5 关 — 死信队列补偿（`dlxConsumer`）**：
- 队列绑定 `x-dead-letter-exchange` / `x-dead-letter-routing-key`
- 失败订单进 `order.dlx.queue`，消费者 **increment 回滚 Redis 库存** + 记录日志

**追问方向**：
- Q：为什么不直接线程池异步？→ MQ 有持久化（服务重启不丢）、削峰（prefetch 控制速率）、死信兜底
- Q：不超卖怎么保证？→ Lua 原子性 + DB 乐观锁 `gt(stock,0)` + 幂等校验三层
- Q：消息丢失怎么办？→ 生产者 Confirm/Returns + Redis 记录补偿 + 消费者手动 ACK + 死信
- Q：为什么用 SET 不用 INCRBY？→ INCRBY 对脏数据（非整型）直接报错，SET 容错更高
- Q：Redis 库存与 DB 库存一致性？→ Redis 预扣，DB 落库，最终一致（死信补偿）

---

### 考点 6：Feed 流 + 点赞 + 签到 + GEO（Redis 数据结构秀肌肉）

**Feed 流（推模式）**：
- 博主发笔记 → 遍历粉丝，写入每个粉丝的 ZSet `feed:{userId}`，score=时间戳
- 滚动分页：`REVRANGEBYSCORE key max min offset count`，返回 `minTime + offset` 翻页
- **为什么不用 Page 分页？** 新增数据会挤掉分页位置，导致重复/丢失 → 用游标方式

**点赞**：
- ZSet `blog:liked:{blogId}`，score=点赞时间戳
- 点赞排行榜：`ZRANGE key 0 4` 取 top5，DB `FIELD()` 保序
- 点赞数用 DB `liked = liked ± 1`，ZSet 只存点赞用户

**签到（BitMap）**：
- key=`sign:{userId}:{yyyyMM}`，offset=当月第几天-1，`SETBIT` 签到
- 统计连续签到：`BITFIELD get u{day}` 取出当月位图 → `&1` + `>>>` 循环计数
- **优点**：1 个用户 1 年只占 46 字节，内存极小

**GEO 附近搜索**：
- 商户坐标写入 `shop:geo:{typeId}`，`GEOSEARCH` 5000 米 + includeDistance
- 手动分页：skip + limit；DB `ORDER BY FIELD(id,...)` 保持距离排序
- fallback：GEO 无数据时降级普通分页查询

**关注/取关**：Redis Set `follow:{userId}` + DB 记录；**共同关注** = `SINTER` 两集合交集

**追问方向**：
- Q：Feed 推模式 vs 拉模式？→ 推：延迟低但粉丝多时写放大；拉：读放大；大 V 可用推拉结合
- Q：BitMap 一天多少人签到内存多少？→ 每人每年 46 字节，10 万用户约 4.4MB

---

## 三、其他模块考点（次要但可能问）

### 私信系统
- 表结构：`message(id, sender_id, receiver_id, content, is_read, create_time)`
- 聊天记录：双向查询（A→B OR B→A）按时间升序
- 会话列表：取与每个用户**最新一条消息**做预览 + 未读数统计（Map 去重）
- 读后置已读（查询历史时批量 update）

### 通知系统
- 点赞/关注/评论触发写入 `notification` 表，列表按时间倒序 limit 50

### 统一异常处理
- `@RestControllerAdvice` + `@ExceptionHandler(RuntimeException)` → 统一返回 `Result.fail("服务器异常")`

---

## 四、压测调优数据（背数字）

**环境**：单机虚拟机，JMeter 5.6.3，接口 `/voucher-order/seckill/10` 经 Nginx `/api/` 代理

| 轮次 | 关键操作 | Error% |
|:---|:---|:---|
| 1 | Redis 脏数据导致 INCRBY 崩溃 | 100% |
| 2 | 改 SET 扣减 | 50% |
| 3 | 限流未关 + 连接池 10 | 33.3% |
| 4 | 连接池 200 | 25% |
| 5 | Tomcat/HikariCP/MQ 调优 | 25% |
| 6 | 连接池 400、Tomcat 500、拦截器去重、限流全关 | 14.4% |
| 7 | TCP 洪峰（somaxconn=128） | 14.4% |
| 8 | Ramp-up 0→5s | 9.09% |
| 9 | Ramp-up 0→10s | 7.89% |

**调优结论（背下来）**：
1. Error% 是 JMeter TCP 连接损耗，不是 HTTP 500 —— 所有到达服务端的请求 `success:true`
2. Ramp-up 线性改善网络洪峰：14.4% → 9.09% → 7.89%
3. 代码层调优点：INCRBY→SET 根治崩溃、拦截器去重减 33% Redis 开销、连接池扩容消除超时
4. 业务代码已无明显瓶颈，瓶颈在网络层

**生产调优参数**：Tomcat max=500 / Redis pool max-active=400 / HikariCP max=50 / MQ prefetch=50、并发 20~50

---

## 五、快问快答 30 条（面试速刷）

1. **项目是什么** → 本地生活点评平台，含秒杀/缓存/GEO/社区
2. **秒杀怎么防超卖** → Lua 原子扣减 + DB `WHERE stock>0` + 幂等校验
3. **秒杀为什么用 MQ** → 削峰、持久化可靠、死信兜底、解耦
4. **消息可靠性** → 生产者 Confirm/Returns + 手动 ACK + 死信 + Redis 补偿
5. **缓存穿透** → 空对象缓存 / 布隆过滤器
6. **缓存击穿** → 互斥锁 / 逻辑过期
7. **缓存雪崩** → 过期随机值 / 多级缓存 / 限流降级
8. **互斥锁 vs 逻辑过期** → 一致性 vs 性能
9. **分布式锁演进** → SETNX 自研 → Lua 解锁 → Redisson 看门狗
10. **为什么解锁用 Lua** → 判断+删除需原子
11. **分布式 ID** → 时间戳 32 位 | 序列号 32 位，趋势递增
12. **序列号为什么带日期** → 每天独立计数防溢出
13. **Feed 流用什么** → ZSet 推模式 + 滚动分页
14. **为什么不用 Page 分页 Feed** → 新增数据导致重复/丢失
15. **点赞排行榜** → ZSet score=时间戳，top5 + FIELD 保序
16. **签到内存** → BitMap 每人每年 46 字节
17. **连续签到怎么算** → BITFIELD + 位运算 &1 + >>>
18. **附近搜索** → GEO + GEOSEARCH + 手动分页 + fallback
19. **共同关注** → SINTER 集合交集
20. **Token 为什么存 Redis** → 集群 Session 共享
21. **双拦截器作用** → 刷 TTL + 校验登录态，order 0/1
22. **ThreadLocal 隐患** → 线程复用需 remove
23. **为什么 SET 不用 INCRBY** → 脏数据兼容
24. **限流实现** → ZSet 滑动窗口 Lua，每秒每用户 5 次
25. **死信队列** → 失败消息进 DLX，回滚 Redis 库存
26. **压测错误率** → 100% → 7.89%，瓶颈在网络层
27. **验证码** → Redis 存 2 分钟，手机号做 key
28. **私信已读** → 查询历史时批量置已读
29. **幂等设计** → 消费者查重 user_id+voucher_id
30. **项目最大难点** → 秒杀高并发 + 缓存一致性 + 消息可靠性

---

## 六、刁钻题防坑清单（易被追问翻车点）

1. **"你的秒杀库存 Redis 和 DB 会不一致吗？"**
   → 会短暂不一致：Redis 预扣 → MQ 异步落库 → 最终一致。消费者失败 → 死信 → increment 回滚 Redis 库存补偿。

2. **"限流被注释了？"**
   → 压测时临时关闭（避免误伤压测流量），生产环境开启。面试话术：压测验证的是核心链路吞吐，限流逻辑本身有单测。

3. **"逻辑过期返回旧数据，一致性怎么保证？"**
   → 牺牲强一致换取高可用，30 分钟内自动修复；对商户详情这类读多写少的数据可接受。需要强一致场景换互斥锁方案。

4. **"Redis 挂了怎么办？"**
   → 缓存层面：穿透防护为空返回；GEO 有 DB fallback。生产可上哨兵/集群（项目已预留哨兵配置注释）。

5. **"消息消费重复怎么办？"**
   → 消费者查 DB 幂等校验，重复消息直接丢弃；配合手动 ACK。

6. **"为什么一人一单用 Redis SISMEMBER 而不是查 DB？"**
   → Lua 内原子完成，避免高并发下 DB 压力；DB 幂等校验作为消费者侧兜底。

7. **"点赞数 DB 和 Redis 怎么同步？"**
   → ZSet 存用户（判重/排行），DB `liked` 字段计数；先 DB 后 Redis，失败由 ZSet score 判空兜底重试。

---

## 七、一句话亮点总结（结尾升华）

> 项目最大的收获是**完整走通了一条高并发系统的演进路线**：
> 从最简单的缓存 → 缓存三兄弟（穿透/击穿/雪崩）逐一攻破 → 自研分布式锁 → Redisson →
> 秒杀从纯 DB 改为 **Lua 原子 + MQ 异步 + 死信补偿** → 9 轮压测把错误率从 100% 压到 7.89%。
>
> 每一步都是基于线上实际压测数据驱动的优化，而不是空谈理论。
