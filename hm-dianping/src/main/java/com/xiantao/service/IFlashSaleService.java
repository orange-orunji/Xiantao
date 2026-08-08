package com.xiantao.service;

import com.xiantao.dto.Result;
import com.xiantao.entity.FlashSale;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IFlashSaleService extends IService<FlashSale> {

    Result queryFlashSaleOfGoods(Long goodsId);

    void addFlashStock(FlashSale flash);
}
