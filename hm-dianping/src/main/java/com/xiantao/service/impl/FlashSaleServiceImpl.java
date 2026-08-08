package com.xiantao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiantao.dto.Result;
import com.xiantao.entity.FlashSale;
import com.xiantao.mapper.FlashSaleMapper;
import com.xiantao.entity.FlashStock;
import com.xiantao.service.IFlashStockService;
import com.xiantao.service.IFlashSaleService;
import com.xiantao.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class FlashSaleServiceImpl extends ServiceImpl<FlashSaleMapper, FlashSale> implements IFlashSaleService {

    @Resource
    private IFlashStockService flashStockService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryFlashSaleOfGoods(Long goodsId) {
        // 查询该商品的捡漏活动
        List<FlashSale> flashs = getBaseMapper().queryFlashSaleOfGoods(goodsId);
        // 返回结果
        return Result.ok(flashs);
    }

    @Override
    @Transactional
    public void addFlashStock(FlashSale flash) {
        // 保存捡漏活动
        save(flash);
        // 保存捡漏库存信息
        FlashStock flashStock = new FlashStock();
        flashStock.setFlashId(flash.getId());
        flashStock.setStock(flash.getStock());
        flashStock.setBeginTime(flash.getBeginTime());
        flashStock.setEndTime(flash.getEndTime());
        flashStockService.save(flashStock);
        // 缓存中添加捡漏库存
        stringRedisTemplate.opsForValue().set(RedisConstants.FLASH_STOCK_KEY+flash.getId(), flash.getStock().toString());
    }
}
