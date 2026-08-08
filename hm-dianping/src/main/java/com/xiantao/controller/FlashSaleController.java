package com.xiantao.controller;


import com.xiantao.dto.Result;
import com.xiantao.entity.FlashSale;
import com.xiantao.service.IFlashSaleService;
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
@RequestMapping("/flash-sale")
public class FlashSaleController {

    @Resource
    private IFlashSaleService flashService;

    /**
     * 新增普通券
     * @param flash 优惠券信息
     * @return 优惠券id
     */
    @PostMapping
    public Result addFlashSale(@RequestBody FlashSale flash) {
        flashService.save(flash);
        return Result.ok(flash.getId());
    }

    /**
     * 新增秒杀券
     * @param flash 优惠券信息，包含秒杀信息
     * @return 优惠券id
     */
    @PostMapping("seckill")
    public Result addFlashStock(@RequestBody FlashSale flash) {
        flashService.addFlashStock(flash);
        return Result.ok(flash.getId());
    }

    /**
     * 查询店铺的优惠券列表
     * @param goodsId 店铺id
     * @return 优惠券列表
     */
    @GetMapping("/list/{goodsId}")
    public Result queryFlashSaleOfGoods(@PathVariable("goodsId") Long goodsId) {
       return flashService.queryFlashSaleOfGoods(goodsId);
    }
}
