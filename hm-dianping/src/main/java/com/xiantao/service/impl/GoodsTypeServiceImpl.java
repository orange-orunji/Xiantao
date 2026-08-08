package com.xiantao.service.impl;

import cn.hutool.json.JSONUtil;
import com.xiantao.entity.GoodsType;
import com.xiantao.mapper.GoodsTypeMapper;
import com.xiantao.service.IGoodsTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiantao.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class GoodsTypeServiceImpl extends ServiceImpl<GoodsTypeMapper, GoodsType> implements IGoodsTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private GoodsTypeMapper goodsTypeMapper;

    @Override
    public List<GoodsType> queryList() {
        //TODO 在redis中查询商店类型数据
        String s = stringRedisTemplate.opsForValue().get(RedisConstants.CASH_GOODS_TYPE_KEY);
        //TODO 存在则返回
        if (s != null) {
            return JSONUtil.toList(s, GoodsType.class);
        }
        //TODO 不存在，查询数据库
        List<GoodsType> typeList = goodsTypeMapper.selectList(null);
        //TODO 存在，写入redis
        String jsonStr = JSONUtil.toJsonStr(typeList);
        stringRedisTemplate.opsForValue().set(RedisConstants.CASH_GOODS_TYPE_KEY, jsonStr, RedisConstants.CACHE_GOODS_TTL);
        //TODO 返回
        return typeList;
    }
}
