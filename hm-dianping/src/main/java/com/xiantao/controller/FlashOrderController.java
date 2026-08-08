package com.xiantao.controller;


import com.xiantao.dto.Result;
import com.xiantao.entity.FlashOrder;
import com.xiantao.service.IFlashOrderService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/flash-order")
public class FlashOrderController {
    @Resource
    private IFlashOrderService flashOrderService;

    /**
     * 捡漏抢购
     * @param flashId
     * @return
     */
    @PostMapping("seckill/{id}")
    public Result seckillFlashSale(@PathVariable("id") Long flashId) throws InterruptedException {
        Object result = flashOrderService.seckillFlashSale(flashId);
        if (result instanceof Result) {
            return (Result) result;
        }
        return Result.ok(result);
    }

    /**
     * 轮询接口，查询当前订单是否完成抢购
     * @param orderId
     * @return
     * 前端轮询逻辑：
     *   GET /flash-order/{orderId}
     *     ├─ 返回 null         → "处理中，继续等" → 1 秒后再查
     *     ├─ status = 1        → "下单成功！" → 停止轮询
     *     ├─ status = 4        → "下单失败" → 停止轮询
     *     └─ status = 2/3/5/6  → 对应展示
     */
    @GetMapping("/{orderId}")
    public Result queryOrder(@PathVariable Long orderId){
        FlashOrder order = flashOrderService.getById(orderId);
    if (order == null) {
        // 订单不存在或处理失败（被死信吃掉），直接返回失败状态码
        return Result.fail("订单处理失败");
    }
    return Result.ok(order.getStatus());
    }

    /**
     * 查询当前用户的订单列表（个人主页"我的订单"）
     * @return 订单列表
     */
    @GetMapping("/mine")
    public Result queryMyOrders() {
        return flashOrderService.queryMyOrders();
    }

    /**
     * 查询当前用户各状态订单数量（个人主页状态快捷入口角标）
     * @return 状态 -> 数量 映射
     */
    @GetMapping("/status-count")
    public Result queryOrderStatusCount() {
        return flashOrderService.queryOrderStatusCount();
    }
}
