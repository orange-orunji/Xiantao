package com.xiantao.utils;

import com.xiantao.config.RabbitMqConfig;
import com.xiantao.entity.FlashOrder;
import com.xiantao.exception.StockEmptyException;
import com.xiantao.service.impl.FlashOrderServiceImpl;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
public class OrderConsumer {

    @Resource
    private FlashOrderServiceImpl flashOrderService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 订单队列消费者
     *
     * @param flashOrder
     * @param message
     * @param channel
     * @throws IOException ┌─────────────────────────────────────────────────────────┐
     *                     │                    消息处理流程                           │
     *                     │                                                         │
     *                     │  RabbitMQ ──消息──▶ order.queue ──▶ OrderConsumer        │
     *                     │                                          │               │
     *                     │                            ┌─────────────┴──────────┐    │
     *                     │                            ▼                        ▼    │
     *                     │                       成功 ✓                    失败 ✗   │
     *                     │                            │                        │    │
     *                     │                     basicAck()              basicNack()  │
     *                     │                            │                        │    │
     *                     │                      消息删除               requeue=?    │
     *                     │                                        ┌───────┴──────┐ │
     *                     │                                      true           false│
     *                     │                                       │               │  │
     *                     │                                  重回原队列      路由到 DLX │
     *                     │                                  (可能死循环!)      │      │
     *                     │                                                   ▼      │
     *                     │                                           order.dlx.queue│
     *                     │                                                   │      │
     *                     │                                              dlxConsumer │
     *                     │                                             记录日志/告警  │
     *                     └─────────────────────────────────────────────────────────┘
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    key = "order.generate",                                  //绑定遗嘱给死信队列
                    value = @Queue(value = "order.queue", durable = "true", arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = "order.dlx.exchange"),
                            @Argument(name = "x-dead-letter-routing-key", value = "order.dlx")
                    }),
                    exchange = @Exchange(value = "order.exchange", type = ExchangeTypes.DIRECT)
            )
    )
    public void orderConsumer(FlashOrder flashOrder,
                              Message message,
                              Channel channel) throws IOException {
        long consumerTag = message.getMessageProperties().getDeliveryTag();
        try {
            flashOrderService.getFlashOrder(flashOrder);
        } catch (StockEmptyException e) {
            // DB 库存不足：说明 Redis 与 DB 库存不一致，直接确认消息，不回补库存（回补会放大超卖），告警人工核查
            log.error("【库存不一致】DB 库存不足，订单 {} 不回补 Redis 库存，需人工核查，flashId={}",
                    flashOrder.getId(), flashOrder.getFlashId(), e);
            channel.basicAck(consumerTag, false);
            return;
        } catch (Exception e) {
            log.error("订单处理失败，订单ID：{}", flashOrder.getId(), e);
            channel.basicNack(consumerTag, false, false);
            return;
        }
        channel.basicAck(consumerTag, false);
    }

    /**
     * 死信队列
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    key = "order.dlx",
                    value = @Queue(value = "order.dlx.queue", durable = "true"),
                    exchange = @Exchange(value = "order.dlx.exchange", type = ExchangeTypes.DIRECT)
            )
    )
    public void dlxConsumer(FlashOrder flashOrder, Message message, Channel channel) throws IOException {
        long consumerTag = message.getMessageProperties().getDeliveryTag();
        try {
            // 1.补偿幂等：订单ID（雪花ID，全局唯一）为幂等键，SETNX 抢不到说明已补偿过，直接确认（manual 模式下消息会重复投递，无幂等会导致库存虚增→超卖）
            Boolean first = stringRedisTemplate.opsForValue()
                    .setIfAbsent(RedisConstants.ORDER_DLX_COMPENSATED_KEY + flashOrder.getId(), "1", Duration.ofHours(24));
            if (Boolean.FALSE.equals(first)) {
                log.warn("【死信】订单 {} 已补偿过，跳过重复补偿", flashOrder.getId());
                channel.basicAck(consumerTag, false);
                return;
            }
            log.error("【死信】订单 {} 已进入死信队列，用户ID：{}，活动ID：{}",
                    flashOrder.getId(), flashOrder.getUserId(), flashOrder.getFlashId());
            // 2.回补 Redis 库存
            String stockKey = RedisConstants.FLASH_STOCK_KEY + flashOrder.getFlashId();
            Boolean exists = stringRedisTemplate.hasKey(stockKey);
            if (Boolean.TRUE.equals(exists)) {
                Long stock = stringRedisTemplate.opsForValue().increment(stockKey);
                log.info("已补偿 Redis 库存，key={}，当前库存={}", stockKey, stock);
            } else {
                log.warn("Redis 库存 key 不存在，跳过补偿：{}", stockKey);
            }
            // 3.处理完成，手动确认（manual 模式下不 ACK 会反复重投）
            channel.basicAck(consumerTag, false);
        } catch (Exception e) {
            log.error("死信补偿处理异常，订单ID：{}", flashOrder.getId(), e);
            channel.basicNack(consumerTag, false, true);
        }
    }
}
