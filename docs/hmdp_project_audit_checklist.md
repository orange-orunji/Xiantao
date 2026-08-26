# 闲淘项目 · 深度体检清单

> 体检日期：2026-08-26
> 体检范围：捡漏抢购链路（Lua → MQ → 消费者 → 死信补偿）、压测报告、代码卫生
> 结论先行：**广度与工程化已达标，但秒杀链路存在 1 个真实 bug + 3 个面试硬伤，修复后可达到"经得起追问"的水平。**

---

## P0 · 真实 Bug（必须修，否则面试现场翻车）

### 1. 死信消费者无 ACK + 补偿无幂等 → 库存被重复 +1，导致超卖

**现状**：`OrderConsumer.java` 的 `dlxConsumer` 没有 `Channel` 参数，无法调用 `basicAck`；而 `application.yaml` 全局配置 `acknowledge-mode: manual`。

**为什么是问题**：manual 模式下未 ACK 的消息在通道关闭 / prefetch 耗尽时会重新投递 → `dlxConsumer` 被反复执行 → Redis 库存被反复 `increment` +1。库存虚增 = 超卖，这是生产事故级 bug。

**怎么修**：
```java
public void dlxConsumer(FlashOrder flashOrder, Message message, Channel channel) throws IOException {
    // 1. 补偿幂等：orderId 是消息唯一键，先 SETNX，抢不到说明已补偿过，直接 ACK
    Boolean first = stringRedisTemplate.opsForValue()
            .setIfAbsent("order:dlx:compensated:" + flashOrder.getId(), "1", Duration.ofHours(24));
    if (Boolean.FALSE.equals(first)) {
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        return;
    }
    // 2. 再补偿库存
    ...
    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
}
```

**面试官会问**：死信队列的消息会不会重复消费？你用什么保证补偿只执行一次？
**你现在的答案**：只能答"不会重复"——代码上不成立。修完后：SETNX 幂等键 + 手动 ACK 双保险。

---

### 2. 死信补偿不区分失败原因 → 可能把库存虚增

**现状**：`dlxConsumer` 无条件 `increment(stockKey)`。但 `getFlashOrder` 抛异常的原因至少有三种：
- 库存不足（DB `stock=0`，`gt(stock,0)` 失败）→ **Redis 扣的库存不该回补**，回补反而虚增
- 订单落库失败（库存已扣、save 失败）→ 回补正确
- 网络抖动 / 消费者崩溃 → 回补正确

**为什么是问题**：第一种情况回补 +1 后，Redis 库存比 DB 多 1，下一次秒杀按 Redis 放行 → 超卖。

**怎么修（最小改动）**：让异常携带类型——`getFlashOrder` 里"库存不足"单独抛 `StockEmptyException`；`orderConsumer` 的 catch 里对 `StockEmptyException` 走**不回补**分支（直接 ACK + 记录），其余异常才 Nack 进死信。

**面试官会问**：如果死信是"库存不足"造成的，补偿会不会把库存加多了？
**你现在的答案**：答不了，因为代码确实会加多。

---

### 3. 数据库密码明文提交

**现状**：`application.yaml` 第 16、25、44 行，注释写着"密码通过环境变量注入，请勿提交明文密码"，但密码 `Ww2301079399@`、`123` 就写在文件里，且已推上 GitHub 公开仓库。

**为什么是问题**：公开仓库里的明文数据库密码 = 安全问题；面试官看代码时看到"注释说不要提交 + 实际提交了"会觉得工程素养有问题。

**怎么修**：
1. 改 `${MYSQL_PASSWORD}` / `${REDIS_PASSWORD}` 占位符
2. 立即修改服务器上的真实密码（已泄露，改占位符不够）
3. 用 `git filter-repo` 清理历史提交里的密码，或直接重建仓库

---

### 4. Redis 库存初始化存在竞态

**现状**：`FlashOrderServiceImpl.java` 第 246-251 行：`hasKey` 判断 + `set` 两步非原子。

**为什么是问题**：两个并发请求同时发现 key 不存在，同时执行 `set`。当前写的是同一个值（DB 的 stock），危害小，但这属于"经典非原子检查-设置"模式，面试官看到必然问。

**怎么修**：用 `setIfAbsent`（SETNX），一步完成"不存在才写"。

---

## P1 · 面试硬伤（不修会答崩，但不至于翻车）

### 5. 幂等是"业务级幂等"，不是"消息级幂等"

**现状**：`getFlashOrder` 用 `user_id + flash_id` 查库判重，语义是"一人一单"。

**为什么是问题**：面试官会问——"你的幂等防的是消息重复投递，还是用户重复购买？"。真相是：你用业务规则（一人一单）掩盖了消息幂等，两者恰好结果一致。但追问就露馅：
- 如果订单被取消，用户想再次抢购 → `count>0` 永远成立，**永远被拦**
- `flash:order:{flashId}` 这个 Set 在 Redis 里永久保留，用户一生只能买一次
- 消息级幂等的正确唯一键是 **orderId**（你在 Lua 里生成、随消息携带），应该用它做唯一索引 / 判重

**怎么修**：`flash_order` 表加 `uk_order_id` 唯一索引（orderId 已是雪花 ID，天然唯一），消费者用 `INSERT ... ON DUPLICATE KEY` 或先按 orderId 查；一人一单的"业务限制"单独用 `user_id + flash_id` 唯一索引表达。两个概念分开。

**面试官会问**：orderId 是生产者生成的，为什么不用它做幂等键？

---

### 6. 压测报告缺核心指标，且结论缺证据

**现状**：`pressure-test-report.md` 只有 Error%，**没有 TPS/QPS、RT 平均值、P95/P99**；README 却写着"QPS 3000+"——报告里根本没有这个数字。另外"Error% 来自 TCP 连接损耗"的结论没有抓包或对比实验支撑。

**为什么是问题**：面试官看到"QPS 3000"第一反应是问"3000 怎么来的"，你拿不出报告数字，等于自爆。

**怎么修**：
1. 补一轮压测：固定参数（并发 500、时长 60s、限流全开或标注清楚），用 JMeter 的 Summary Report 导出 **Throughput、Average、P99**
2. 把报告和 README 的数字对齐，或删掉 README 的 QPS 声明
3. "TCP 损耗"结论要么补证据（如 netstat 统计 SYN_RECV / 失败请求日志），要么改写为"待进一步定位"

---

### 7. 压测条件与生产链路不符

**现状**：第 6-9 轮把限流、一人一单、拦截器全关、日志降到 warn 才得到 7.89%。

**为什么是问题**：面试官会问"生产上你会关限流吗？"——你测的是 Lua 裸性能，不是完整链路性能。另外样本只有 500-1000 请求，JMeter 与后端跨虚拟机网络，误差大。

**怎么修**：保留一组"生产配置压测"（限流开启、拦截器开启），哪怕数字难看（比如 QPS 500），也比"裸测 3000"有价值——它能让你讲出"限流对 QPS 的影响有多大"。

---

## P2 · 代码卫生（影响第一印象）

### 8. FlashOrderServiceImpl 有 ~250 行注释掉的演进代码

**现状**：第 89-208 行是注释掉的 Redis Stream + 线程池实现，第 274-361 行是注释掉的旧版秒杀逻辑。

**为什么是问题**：面试官让讲项目时如果现场打开源码，满屏注释代码显得仓库没有版本管理意识。这些演进版本应该活在 git 历史里，而不是注释里。

**怎么修**：删除注释代码（git 历史可找回），保留一个简短的"演进说明"注释指向 commit。

### 9. 测试类形同虚设

**现状**：`FlashOrderServiceTest.java` 只有 `System.out.println`，没有断言。

**怎么修**：至少补 2-3 个有断言的测试：
- `seckill.lua` 单测：库存不足返回 1、重复下单返回 2、成功返回 0 且库存 -1
- 幂等单测：同用户+同券重复调用 `getFlashOrder`，只落库 1 条
- 面试加分句："核心链路有单测覆盖"

---

## P3 · 加分项（P0-P2 做完再做）

### 10. 交易闭环
README"未来规划"自己列的：想要 → 下单 → 状态流转 → 取消回补库存 → 超时关单（延迟队列）。做完后"补偿"这个词才有完整业务语境。

### 11. 缓存一致性策略
商品缓存目前是预热 + 空对象 + 逻辑过期。面试高频题"更新 DB 和删缓存的顺序"，把"延迟双删 / 先更后删失败场景 / 为什么不用 Canal"写成文档进 `interview-cheatsheet.md`，而不是停留在嘴上。

---

## 修复顺序建议

| 阶段 | 内容 | 预计工作量 |
|---|---|---|
| 第 1 周 | P0-1（死信 ACK + 幂等）、P0-2（区分失败原因） | 2 个下午 |
| 第 1 周 | P0-3（密码）、P0-4（SETNX） | 1 个下午 |
| 第 2 周 | P1-5（消息级幂等重构） | 1-2 天 |
| 第 3 周 | P1-6、P1-7（压测重做 + 报告重写） | 1-2 天 |
| 穿插 | P2（删注释、补测试） | 碎片时间 |
| 之后 | P3（交易闭环） | 1-2 周 |

> 修复完 P0+P1 后，这个项目的秒杀链路就能支撑住"你做的"三个字——面试官追问到死信补偿、幂等、压测数据，每一问都有真实代码和数字兜底。
