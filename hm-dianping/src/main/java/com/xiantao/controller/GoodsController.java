package com.xiantao.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiantao.dto.Result;
import com.xiantao.entity.Goods;
import com.xiantao.service.IGoodsService;
import com.xiantao.utils.SystemConstants;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Resource
    public IGoodsService goodsService;

    /**
     * 根据id查询商品信息
     * @param id 商品id
     * @return 商品详情数据
     */
    @GetMapping("/{id}")
    public Result queryGoodsById(@PathVariable Long id) {
        // service 已封装 Result（成功/失败），直接透传，避免双重嵌套导致前端取不到商品数据
        return goodsService.queryById(id);
    }

    /**
     * 发布商品
     * @param goods 商品数据
     * @return 商品id
     */
    @PostMapping
    public Result saveGoods(@RequestBody Goods goods) {
        return goodsService.saveGoods(goods);
    }

    /**
     * 更新商品信息
     * @param goods 商品数据
     * @return 无
     */
    @PutMapping
    public Result updateGoods(@RequestBody Goods goods) {
        // 写入数据库
        goodsService.updateGoods(goods);
        return Result.ok();
    }

    /**
     * 想要/取消想要商品
     * @param id 商品id
     * @return 结果
     */
    @PutMapping("/want/{id}")
    public Result wantGoods(@PathVariable Long id) {
        return goodsService.wantGoods(id);
    }

    /**
     * 查询想要该商品的前5名用户
     * @param id 商品id
     * @return 用户列表
     */
    @GetMapping("/wants/{id}")
    public Result queryGoodsWants(@PathVariable Long id) {
        return goodsService.queryGoodsWants(id);
    }

    /**
     * 查询当前用户想要的商品列表（个人主页"我的想要"）
     * @return 商品列表
     */
    @GetMapping("/want/list")
    public Result queryMyWants() {
        return goodsService.queryMyWants();
    }

    /**
     * 查询当前用户发布的商品列表（个人主页"我的发布"）
     * @return 商品列表
     */
    @GetMapping("/mine")
    public Result queryMyGoods() {
        return goodsService.queryMyGoods();
    }

    /**
     * 查询当前用户最近浏览的商品列表（个人主页"浏览足迹"）
     * @return 商品列表
     */
    @GetMapping("/browse/list")
    public Result queryBrowseList() {
        return goodsService.queryBrowseList();
    }

    /**
     * 根据商品分类分页查询商品信息
     * @param typeId 商品分类
     * @param current 页码
     * @return 商品列表
     */
    @GetMapping("/of/type")
    public Result queryGoodsByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "x" ,required = false) double x,
            @RequestParam(value = "y" ,required = false) double y
    ) {
        return goodsService.queryByPage(typeId,current,x,y);
    }

    /**
     * 根据商品名称关键字分页查询商品信息
     * @param name 商品名称关键字
     * @param current 页码
     * @return 商品列表
     */
    @GetMapping("/of/name")
    public Result queryGoodsByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Goods> page = goodsService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }
}
