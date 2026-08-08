package com.xiantao.service;

import com.xiantao.dto.Result;
import com.xiantao.entity.Goods;
import com.baomidou.mybatisplus.extension.service.IService;


public interface IGoodsService extends IService<Goods> {

    /**
     * 根据id查询商品信息
     * @param id 商品id
     * @return 商品详情数据（已封装成功/失败状态）
     */
    Result queryById(Long id);

    /**
     * 发布商品
     * @param goods 商品数据
     * @return 商品id
     */
    Result saveGoods(Goods goods);

    /**
     * 更新商品信息
     * @param goods 商品数据
     * @return 无
     */
    void updateGoods(Goods goods);

    /**
     * 想要/取消想要商品
     * @param id 商品id
     * @return 结果
     */
    Result wantGoods(Long id);

    /**
     * 查询想要该商品的前5名用户
     * @param id 商品id
     * @return 用户列表
     */
    Result queryGoodsWants(Long id);

    /**
     * 查询当前用户想要的商品列表
     * @return 商品列表（含想要时间）
     */
    Result queryMyWants();

    /**
     * 查询当前用户发布的商品列表
     * @return 商品列表
     */
    Result queryMyGoods();

    /**
     * 记录商品浏览足迹
     * @param id 商品id
     */
    void recordBrowse(Long id);

    /**
     * 查询当前用户最近浏览的商品列表
     * @return 商品列表
     */
    Result queryBrowseList();

    /**
     * 根据商铺类型
     * @param typeId
     * @param current
     * @param size
     * @param distance
     * @return
     */
    Result queryByPage(Integer typeId, Integer current, double size, double distance);
}
