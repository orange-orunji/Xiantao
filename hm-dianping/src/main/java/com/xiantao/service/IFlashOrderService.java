package com.xiantao.service;

import com.xiantao.dto.Result;
import com.xiantao.entity.FlashOrder;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IFlashOrderService extends IService<FlashOrder> {

    Object seckillFlashSale(Long flashId) throws InterruptedException;

    void getFlashOrder(FlashOrder flash);

    /**
     * 查询当前用户的订单列表（关联商品信息）
     * @return 订单列表
     */
    Result queryMyOrders();

    /**
     * 查询当前用户各状态订单数量（个人主页状态快捷入口角标）
     * @return 状态 -> 数量 映射
     */
    Result queryOrderStatusCount();
}
