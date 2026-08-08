package com.xiantao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiantao.entity.FlashSale;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface FlashSaleMapper extends BaseMapper<FlashSale> {

    List<FlashSale> queryFlashSaleOfGoods(@Param("goodsId") Long goodsId);
}
