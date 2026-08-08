package com.xiantao.service;

import com.xiantao.entity.GoodsType;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IGoodsTypeService extends IService<GoodsType> {

    /**
     * 查询所有商铺类型
     * @return 商铺类型列表
     */
    List<GoodsType> queryList();
}
