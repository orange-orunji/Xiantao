package com.xiantao.controller;


import com.xiantao.dto.Result;
import com.xiantao.entity.GoodsType;
import com.xiantao.service.IGoodsTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/goods-type")
public class GoodsTypeController {
    @Resource
    private IGoodsTypeService typeService;

    public GoodsTypeController(IGoodsTypeService typeService) {
        this.typeService = typeService;
    }

    @GetMapping("list")
    public Result queryTypeList() {
//        基于MyBatisPlus单表查询商店类型数据
//        List<GoodsType> typeList = typeService
//                .query().orderByAsc("sort").list();
//      基于redis来查询商店类型数据
        List<GoodsType> typeList = typeService.queryList();
        return Result.ok(typeList);
    }
}
