package com.xiantao.service;

import com.xiantao.entity.FlashOrder;
import com.xiantao.entity.FlashStock;
import com.xiantao.exception.StockEmptyException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 捡漏抢购核心链路单测（P2-9）
 *
 * 前置条件：本地 Redis / MySQL 已启动，MYSQL_PASSWORD 等环境变量已注入（同 application.yaml）。
 * 测试数据统一使用 TEST_FLASH_ID / TEST_USER_ID 高位值，@AfterEach 清理，不污染业务数据。
 */
@SpringBootTest
public class FlashOrderServiceTest {

    /** 测试专用捡漏活动 id，避开业务数据（当前业务数据为 1、2） */
    private static final Long TEST_FLASH_ID = 99999L;
    private static final Long TEST_USER_ID = 99999L;
    private static final String STOCK_KEY = "flash:stock:" + TEST_FLASH_ID;
    private static final String ORDER_KEY = "flash:order:" + TEST_FLASH_ID;

    private static final DefaultRedisScript<Long> SECKILL;
    static {
        SECKILL = new DefaultRedisScript<>();
        SECKILL.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL.setResultType(Long.class);
    }

    @Autowired
    private IFlashOrderService flashOrderService;
    @Autowired
    private IFlashStockService flashStockService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @AfterEach
    public void cleanup() {
        // 清理 Redis 测试键
        stringRedisTemplate.delete(STOCK_KEY);
        stringRedisTemplate.delete(ORDER_KEY);
        // 清理 DB 测试数据
        flashOrderService.removeById(TEST_FLASH_ID);
        flashStockService.removeById(TEST_FLASH_ID);
        flashOrderService.query().eq("flash_id", TEST_FLASH_ID).list()
                .forEach(o -> flashOrderService.removeById(o.getId()));
    }

    /**
     * seckill.lua 单测：成功返回 0 且库存 -1、重复下单返回 2、库存不足返回 1
     */
    @Test
    public void testSeckillLua() {
        stringRedisTemplate.opsForValue().set(STOCK_KEY, "1");

        // 1. 用户A首次抢购：成功返回 0，库存扣减为 0，用户记入已购 Set
        Long r1 = stringRedisTemplate.execute(SECKILL, Collections.emptyList(),
                TEST_FLASH_ID.toString(), TEST_USER_ID.toString(), "order-1");
        assertEquals(0L, r1, "首次抢购应返回 0");
        assertEquals("0", stringRedisTemplate.opsForValue().get(STOCK_KEY), "抢购成功后库存应扣减为 0");
        assertEquals(Boolean.TRUE, stringRedisTemplate.opsForSet().isMember(ORDER_KEY, TEST_USER_ID.toString()),
                "抢购成功后用户应记入 flash:order Set");

        // 2. 用户A重复抢购：返回 2（一人一单），库存不再变化
        Long r2 = stringRedisTemplate.execute(SECKILL, Collections.emptyList(),
                TEST_FLASH_ID.toString(), TEST_USER_ID.toString(), "order-2");
        assertEquals(2L, r2, "同用户重复下单应返回 2");
        assertEquals("0", stringRedisTemplate.opsForValue().get(STOCK_KEY), "重复下单不应扣减库存");

        // 3. 用户B在库存为 0 时抢购：返回 1（库存不足）
        Long r3 = stringRedisTemplate.execute(SECKILL, Collections.emptyList(),
                TEST_FLASH_ID.toString(), String.valueOf(TEST_USER_ID + 1), "order-3");
        assertEquals(1L, r3, "库存不足应返回 1");

        // 4. 库存 key 不存在时：同样返回 1，不能放行
        stringRedisTemplate.delete(STOCK_KEY);
        Long r4 = stringRedisTemplate.execute(SECKILL, Collections.emptyList(),
                TEST_FLASH_ID.toString(), String.valueOf(TEST_USER_ID + 2), "order-4");
        assertEquals(1L, r4, "库存 key 不存在应返回 1（视为无库存）");
    }

    /**
     * getFlashOrder 幂等单测：同用户 + 同活动重复调用只落库 1 条、库存只扣 1 次；
     * DB 无库存时抛 StockEmptyException（消费端据此走"不回补 Redis"分支）
     */
    @Test
    public void testGetFlashOrderIdempotent() {
        // 准备库存数据：stock = 1
        FlashStock stock = new FlashStock()
                .setFlashId(TEST_FLASH_ID)
                .setStock(1)
                .setBeginTime(LocalDateTime.now().minusDays(1))
                .setEndTime(LocalDateTime.now().plusDays(1));
        flashStockService.save(stock);

        // 1. 首次消费：落库 1 条订单，DB 库存 1 -> 0
        flashOrderService.getFlashOrder(buildOrder(1L));
        long count1 = flashOrderService.query().eq("user_id", TEST_USER_ID).eq("flash_id", TEST_FLASH_ID).count();
        assertEquals(1L, count1, "首次消费应落库 1 条订单");
        assertEquals(0, flashStockService.getById(TEST_FLASH_ID).getStock().intValue(), "首次消费后 DB 库存应为 0");

        // 2. 重复消费（模拟 MQ 重复投递）：幂等拦截，订单仍为 1 条，库存不变
        flashOrderService.getFlashOrder(buildOrder(2L));
        long count2 = flashOrderService.query().eq("user_id", TEST_USER_ID).eq("flash_id", TEST_FLASH_ID).count();
        assertEquals(1L, count2, "重复消费不应新增订单（幂等）");
        assertEquals(0, flashStockService.getById(TEST_FLASH_ID).getStock().intValue(), "重复消费不应再扣库存");

        // 3. 删除库存记录后消费：DB 更新影响行数为 0，应抛 StockEmptyException
        flashStockService.removeById(TEST_FLASH_ID);
        assertThrows(StockEmptyException.class,
                () -> flashOrderService.getFlashOrder(buildOrder(3L)),
                "DB 无库存时应抛 StockEmptyException（消费端不回补 Redis 库存）");
    }

    private FlashOrder buildOrder(Long orderId) {
        return new FlashOrder()
                .setId(orderId)
                .setUserId(TEST_USER_ID)
                .setFlashId(TEST_FLASH_ID)
                .setStatus(1);
    }
}
