package com.xiantao.service.impl;

import com.xiantao.dto.Result;
import com.xiantao.dto.UserDTO;
import com.xiantao.entity.FlashOrder;
import com.xiantao.entity.FlashSale;
import com.xiantao.entity.FlashStock;
import com.xiantao.entity.Goods;
import com.xiantao.exception.StockEmptyException;
import com.xiantao.mapper.FlashOrderMapper;
import com.xiantao.service.IFlashStockService;
import com.xiantao.service.IFlashOrderService;
import com.xiantao.service.IGoodsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiantao.service.IFlashSaleService;
import com.xiantao.utils.RedisConstants;
import com.xiantao.utils.RedisIdWorker;
import com.xiantao.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.xiantao.utils.RedisConstants.FLASH_STOCK_KEY;

@Service
@Slf4j
public class FlashOrderServiceImpl extends ServiceImpl<FlashOrderMapper, FlashOrder> implements IFlashOrderService {

    @Resource
    private IFlashStockService seckill ;
    @Resource
    private IFlashSaleService flashService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private IGoodsService goodsService;
    private static final String STREAM_KEY = "stream.orders";
    private static final String GROUP_NAME = "g1";
    private static final String CONSUMER_NAME = "c1";    //创建lua脚ben
    private static final DefaultRedisScript<Long> SECKILL;
    private static final DefaultRedisScript<Long> LIMIT;
    private final boolean running = true;
    // 消费者组是否已经初始化的标记
    private volatile boolean groupInitialized = false;
    static {
//        初始化秒杀脚本
        SECKILL = new DefaultRedisScript<>();
        SECKILL.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL.setResultType(Long.class);
//        初始化限流脚本
        LIMIT = new DefaultRedisScript<>();
        LIMIT.setLocation(new ClassPathResource("rate_limit.lua"));
        LIMIT.setResultType(Long.class);
    }

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;
/**
 * 线程池相关做法
 */
//===============================================================================================================================
    //新创阻塞队列(JVM虚拟机实现)
//    BlockingQueue<FlashOrder> queue = new ArrayBlockingQueue<>(1024 * 1024);
    //创建线程池
//    private final ExecutorService executor = Executors.newSingleThreadExecutor();

//    创建销毁方法，用于销毁线程池
//    @PreDestroy
//    public void destroy() {
//        running = false;
//        executor.shutdown();
//        try {
//            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
//                executor.shutdownNow();
//            }
//        } catch (InterruptedException e) {
//            executor.shutdownNow();
//            Thread.currentThread().interrupt();
//        }
//    }
    //初始化代理对象和提交线程任务
//    @PostConstruct
//    public void init(){
//        if (!groupInitialized) extracted();
//        executor.submit(runnable);
//    }
    //====================================================================================================================================
    /**
     * 创建消费者组
     * @return
     */
    //==================================================================================================================================
//    private void extracted() {
//        try {
//            // 创建消费者组，从队列开头(0)开始消费
//            stringRedisTemplate.opsForStream()
//                    .createGroup(STREAM_KEY, ReadOffset.from("0"), GROUP_NAME);
//            log.info("消费者组 {} 创建成功", GROUP_NAME);
//        } catch (Exception e) {
//            // 组已经存在，忽略异常
//            log.info("消费者组 {} 已存在，无需重复创建", GROUP_NAME);
//        }
//    }
//========================================================================================================================================
/**
    线程池任务
 @return
 **/
//========================================================================================================================================
//    Runnable runnable = new Runnable() {
//        @Override
//        public void run() {
//            groupInitialized = true;
//
//            while (running){
//                try {
////                    FlashOrder flashOrder = queue.take();
////                    proxy.getFlashOrder(flashOrder);
//                    //1.创建消费者监听对象
//                    List<MapRecord<String, Object, Object>> recordList = stringRedisTemplate.opsForStream().read(
//                            Consumer.from(GROUP_NAME,CONSUMER_NAME),
//                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
//                            StreamOffset.create(STREAM_KEY, ReadOffset.from(">"))
//                    );
//                    //2.获取阻塞队列中的订单消息
//                    if(recordList == null || recordList.isEmpty()) continue;
//                    MapRecord<String, Object, Object> record = recordList.get(0);
//                    //3.获取订单信息
//                    FlashOrder flashOrder = BeanUtil.mapToBean(record.getValue(), FlashOrder.class,true);
//                    proxy.getFlashOrder(flashOrder);
//                    //4.XCAK 处理处理队列中的订单信息
//                    stringRedisTemplate.opsForStream().acknowledge(STREAM_KEY,GROUP_NAME,record.getId());
//                } catch (Exception e) {
//                    handlerPlanting();
//                }
//            }
//        }
//
//
//
//        /**
//         * 处理异常带出来订单队列数据
//         * @return
//         */
//
//        private void handlerPlanting() {
//            while (running){
//                try {
//                    List<MapRecord<String, Object, Object>> recordList = stringRedisTemplate.opsForStream().read(
//                            Consumer.from("g1", "c1"),
//                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
//                            StreamOffset.create(STREAM_KEY, ReadOffset.from("0"))
//                    );
//                    //2.获取阻塞队列中的订单消息
//                    if(recordList == null || recordList.isEmpty()) continue;
//                    MapRecord<String, Object, Object> record = recordList.get(0);
//                    //3.获取订单信息
//                    FlashOrder flashOrder = BeanUtil.mapToBean(record.getValue(), FlashOrder.class,true);
//                    //4.XCAK 处理处理队列中的订单信息
//                    stringRedisTemplate.opsForStream().acknowledge(STREAM_KEY,GROUP_NAME,record.getId());
//                } catch (Exception e) {
//                    log.error("处理pending消息失败！", e);
//                    try {
//                        Thread.sleep(2000L);
//                    } catch (InterruptedException ex) {
//                        Thread.currentThread().interrupt();
//                        break;
//                    }
//                }
//            }
//        }
//    };
//
//
    //创建全局类的代理对象
//    IFlashOrderService proxy;
//==========================================================================================================================================================================================


    /**
     * 秒杀优惠券
     * @param flashId
     * @return
     */
    @Override
    public Result seckillFlashSale(Long flashId) throws InterruptedException {
        // 登录校验
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        long orderId = redisIdWorker.nextId("order");
////=================================判断是否限流,Java+RedisTemplate=================================
//        if (rateLimit(user.getId()) != 1 ){
//            return Result.fail("活动太火爆，请稍后再试");
//        }
////===============================================================================================
////================================滑动窗口限流,Lua脚本原子性处理(压测时注释)==================================
        int limitNum = 5,window = 1000;
        Long l1 = stringRedisTemplate.execute(
                LIMIT,
                Collections.emptyList(),
                user.getId().toString(),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(limitNum),
                String.valueOf(window)
        );
        if(l1 == null ||l1 !=1){
            return Result.fail("活动太火爆，请稍后再试");
        }
////================================================================================
        //1.lua脚本实现秒杀库存,一人一单是否抢购成功
        // 确保Redis库存缓存存在，不存在则从数据库初始化（SETNX 原子写入，避免并发下"检查-设置"竞态重复初始化）
        String stockKey = RedisConstants.FLASH_STOCK_KEY + flashId;
        FlashStock sv = seckill.getById(flashId);
        if (sv != null && sv.getStock() != null) {
            stringRedisTemplate.opsForValue().setIfAbsent(stockKey, sv.getStock().toString());
        }
        Long l = stringRedisTemplate.execute(
                //lua脚本引用
                SECKILL,
                Collections.emptyList(),
                flashId.toString(),
                user.getId().toString(),String.valueOf(orderId)
        );
        long r = l.intValue();
        //2.判断是否抢购成功
        //2.1 失败
        if(r!=0) return Result.fail(r==1?"库存不足！":"请勿重复下单！");
//        if (proxy==null)  proxy = (IFlashOrderService) AopContext.currentProxy();
        FlashOrder flashOrder = new FlashOrder();
        flashOrder.setId(orderId);
        flashOrder.setFlashId(flashId);
        flashOrder.setUserId(user.getId());
        flashOrder.setStatus(1);
        rabbitTemplate.convertAndSend("order.exchange","order.generate",
                flashOrder,new CorrelationData(String.valueOf(orderId)));
                                //CorrelationData消息快递单号,用于给生产者处理确定是什么订单传过来的，便于后续维护
        return Result.ok(orderId);
    }
//    @Override
//    public Result seckillFlashSale(Long flashId) throws InterruptedException {
//        //1.lua脚本实现秒杀库存,一人一单是否抢购成功
//        Long l = stringRedisTemplate.execute(
//                SECKILL,
//                Collections.emptyList(),
//                flashId.toString(),
//                UserHolder.getUser().getId().toString()
//        );
//        long r = l.intValue();
//        //2.判断是否抢购成功
//        //2.1 失败
//        if(r!=0){
//            return Result.fail(r==1?"库存不足！":"请勿重复下单！");
//        }
//        //3. 基于阻塞队列来实现存储
//        long orderId = redisIdWorker.nextId("order");
//        FlashOrder flashOrder = new FlashOrder();
//        flashOrder.setId(orderId);
//        flashOrder.setUserId(UserHolder.getUser().getId());
//        flashOrder.setFlashId(flashId);
//        if (proxy==null)  proxy = (IFlashOrderService) AopContext.currentProxy();
//        queue.put(flashOrder);
//        return Result.ok(orderId);
//    }
    //分布式+lua脚本来实现原子性的秒杀
//    @Override
//    public Object seckillFlashSale(Long flashId) throws InterruptedException {
//        //1.根据id查询优惠券信息
//        FlashStock flash = seckill.getById(flashId);
//        //2.判断秒杀是否开始
//        if(flash.getBeginTime().isAfter(LocalDateTime.now())){
//            return Result.fail("当前秒杀尚未开始！");
//        }
//        //3.判断库存是否充足
//        if(flash.getStock()<1){
//            return Result.fail("库存不足！");
//        }
//        Long userId = UserHolder.getUser().getId();
//        String name = USER_ID + userId;
//        //获取锁对象
////        IRedisLock lock = new IRedisLock(USER_ID+userId, stringRedisTemplate);
//        RLock lock = redissonClient.getLock(name);
//        if (!lock.tryLock(1, 10, TimeUnit.SECONDS)){
//            return Result.fail("请勿重复下单！");
//        }
//        try {
//            //获取事务代理对象
//            IFlashOrderService proxy = (IFlashOrderService) AopContext.currentProxy();
//            return proxy.getFlashOrder(flashId);
//        } catch (IllegalStateException e) {
//            return Result.fail("请勿重复下单！");
//        } finally {
//            lock.unlock();
//        }
//    }

    /**
     * 创建订单
     * @param flash
     * @return
     */
//    @Transactional
//    public void getFlashOrder(FlashOrder flash, Message message, AMQImpl.Channel channel) {
//        //一人一单处理
////        Long userId = UserHolder.getUser().getId();
////        幂等检验,判断是否重复下单
//        Integer count = query().eq("user_id", flash.getUserId()).eq("flash_id", flash.getFlashId()).count();
//        if(count>0){
//            log.error("您已购买过该优惠券！");
//            throw new RuntimeException();
//        }
//        //4.更新库存
//        seckill.update().setSql("stock = stock - 1")
//                .eq("flash_id", flash.getFlashId())
//                .gt("stock",0)
//                .update();
////        //5.创建订单
////        FlashOrder order = new FlashOrder();
////        //6.设置订单属性
////        order.setFlashId(flashId);
////        order.setUserId(userId);
////        order.setId(redisIdWorker.nextId("order"));
////        //8.保存订单
//        save(flash);
////        //9.返回订单结果
////        Result.ok(flash.getId());
//    }

    /**
     * 基于rabbitMq来实现秒杀活动
     * @param flash
     */
    @Transactional
    public void getFlashOrder(FlashOrder flash) {
        // 幂等校验,判断是否重复下单
        Integer count = query().eq("user_id", flash.getUserId()).eq("flash_id", flash.getFlashId()).count();
//        重复购买
        if(count > 0){
            log.error("您已购买过该优惠券");
            return;
        }
        // 更新秒杀券库存
        boolean success = seckill.update()
                .setSql("stock = stock - 1")
                .eq("flash_id", flash.getFlashId())
                .gt("stock", 0)
                .update();
        if (!success) {
            log.error("库存不足，flashId={}", flash.getFlashId());
            // 单独抛 StockEmptyException：DB 无库存说明 Redis 与 DB 不一致，
            // 消费端不回补 Redis 库存（回补会放大超卖），需告警人工核查
            throw new StockEmptyException("库存不足，flashId=" + flash.getFlashId());
        }
        // 保存订单
        save(flash);
    }

    /**
     * 查询当前用户的订单列表（关联捡漏活动与商品信息）
     * @return 订单列表
     */
    @Override
    public Result queryMyOrders() {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        List<FlashOrder> orders = query().eq("user_id", user.getId())
                .orderByDesc("create_time").list();
        if (orders == null || orders.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 关联捡漏活动（取商品id与捡漏价）
        List<Long> flashIds = orders.stream().map(FlashOrder::getFlashId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, FlashSale> flashMap = new HashMap<>();
        if (!flashIds.isEmpty()) {
            flashService.query().in("id", flashIds).list()
                    .forEach(f -> flashMap.put(f.getId(), f));
        }
        // 关联商品信息
        List<Long> goodsIds = flashMap.values().stream().map(FlashSale::getGoodsId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Goods> goodsMap = new HashMap<>();
        if (!goodsIds.isEmpty()) {
            goodsService.query().in("id", goodsIds).list()
                    .forEach(g -> goodsMap.put(g.getId(), g));
        }
        // 组装订单视图
        List<Map<String, Object>> result = orders.stream().map(o -> {
            Map<String, Object> item = new HashMap<>();
            item.put("orderId", o.getId());
            item.put("status", o.getStatus());
            item.put("createTime", o.getCreateTime());
            FlashSale fs = flashMap.get(o.getFlashId());
            Goods g = fs == null ? null : goodsMap.get(fs.getGoodsId());
            item.put("flashTitle", fs == null ? null : fs.getTitle());
            item.put("flashPrice", fs == null ? null : fs.getPrice());
            item.put("goods", g);
            return item;
        }).collect(Collectors.toList());
        return Result.ok(result);
    }

    /**
     * 查询当前用户各状态订单数量（个人主页状态快捷入口角标）
     * @return 状态 -> 数量 映射，如 {1: 2, 2: 1, 3: 1}
     */
    @Override
    public Result queryOrderStatusCount() {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        List<FlashOrder> orders = query().eq("user_id", user.getId()).select("status").list();
        Map<String, Long> countMap = new HashMap<>();
        for (FlashOrder o : orders) {
            if (o.getStatus() == null) {
                continue;
            }
            countMap.merge(String.valueOf(o.getStatus()), 1L, Long::sum);
        }
        return Result.ok(countMap);
    }
//
//    /**
//     * 秒杀活动限流
//     * @param userId
//     * @return
//     */
//    public Integer rateLimit(Long userId){
//        String key = "rate_limit:skill:" + userId;
//        long l = System.currentTimeMillis() ;
////        移除一秒前的计数
//        stringRedisTemplate.opsForZSet().removeRange(key, 0, l - 1000);
//        Long card = stringRedisTemplate.opsForZSet().zCard(key);
////        限制每秒请求5
//        if (card == null || card > 5) {
//            return 0;
//        }
////        更新redis并放行
//        stringRedisTemplate.opsForZSet().add(key, String.valueOf(userId), l);
//        stringRedisTemplate.expire(key, 2, TimeUnit.SECONDS);
//        return 1;
//    }

    @Override
    public Result payOrder(Long orderId, Integer payType) {
        if (orderServer(orderId)) return Result.fail("订单不存在");
        if(!update().set("status", 2).set("pay_time", LocalDateTime.now())
                .set("pay_type", payType).eq("id", orderId)
                .eq("status", 1).update()){
            return Result.fail("订单状态已变化，请刷新重试");
        }
        log.info("订单已支付，orderId={}", orderId);
        return Result.ok("支付成功");
    }

    /**
     * 取消订单
     * @param orderId 订单ID
     * @return 结果
     */
    @Transactional
    @Override
    public Result cancelOrder(Long orderId) {
        FlashOrder flashOrder = getById(orderId);
        if (flashOrder == null) {
            return Result.fail("订单不存在");
        }
        UserDTO user = UserHolder.getUser();
        if (user == null || !user.getId().equals(flashOrder.getUserId())) {
            return Result.fail("订单不存在");
        }
        if (!update().set("status", 4).eq("id", orderId).eq("status", 1).update()) {
            return Result.fail("订单状态已变化，请刷新重试");
        }
        seckill.update().setSql("stock = stock + 1").eq("flash_id", flashOrder.getFlashId()).update();
        stringRedisTemplate.opsForValue().increment(FLASH_STOCK_KEY + flashOrder.getFlashId(), 1);
        log.info("订单已取消，orderId={}", orderId);
        return Result.ok("订单取消成功");
    }

    /**
     * 确认订单
     * @param orderId 订单ID
      * @return  确认订单结果
     */
    @Override
    public Result confirmOrder(Long orderId) {
        if (orderServer(orderId)) return Result.fail("订单不存在");
        if (!update().set("status", 3).eq("id", orderId).eq("status", 2).update()) {
            return Result.fail("订单状态已变化，请刷新重试");
        }
        log.info("订单已确认，orderId={}", orderId);
        return Result.ok("订单已确认");
    }

    private boolean orderServer(Long orderId) {
        FlashOrder flashOrder = getById(orderId);
        if (flashOrder == null)
            return true;
        UserDTO user = UserHolder.getUser();
        return user == null || !user.getId().equals(flashOrder.getUserId());
    }
}
