package com.xiantao.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.xiantao.utils.RedisConstants.*;


@Component
@Slf4j
public class CacheClient {
    private static StringRedisTemplate stringRedisTemplate;
    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        CacheClient.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 公共设置缓存(包含存在时间)方法
     * @param key
     * @param value
     * @param expireTime
     * @param unit
     */
    public void set(String key, Object value, Long expireTime, TimeUnit  unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), expireTime, unit);
    }

    /**
     * 公共设置逻辑过期缓存方法
     * @param key
     * @param value
     * @param expireTime
     * @param unit
     */
    public void setWithLogicalExpire(String key, Object value, Long expireTime, TimeUnit  unit){
        RedisData data = new RedisData();
        data.setData(value);
        data.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(expireTime)));
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(data),expireTime,unit);
    }

    private boolean tryLock(String id){
        return BooleanUtil.isTrue(stringRedisTemplate.opsForValue().setIfAbsent(
                LOCK_GOODS_KEY+id,
                "",
                RedisConstants.LOCK_GOODS_TTL,
                TimeUnit.SECONDS
        ));
    }

    private boolean unLock(String id){
        return BooleanUtil.isTrue(stringRedisTemplate.delete(LOCK_GOODS_KEY+id));
    }

    //获取线程池
    private static final ExecutorService CACHE_THREAD_POOL= Executors.newFixedThreadPool(10);

    /**
     * 逻辑锁解决缓存击穿问题
     * @param prefix
     * @param id
     * @param type
     * @param expireTime
     * @param unit
     * @param dbFallback
     * @param <R>
     * @param <ID>
     * @return
     */
    public <R,ID> R queryWithExpireTime(String prefix,ID id,Class<R> type,
                                        Long expireTime,TimeUnit unit,Function<ID, R> dbFallback) {
        //1.查询redis是否存在店铺信息
        String sp = stringRedisTemplate.opsForValue().get(prefix + id);
        //2.不存在返回,说明不是热门商品
        if(StrUtil.isBlank(sp)){
            return null;
        }
        //3.数据不为空
        RedisData data = BeanUtil.toBean(sp, RedisData.class);
        R result = BeanUtil.toBean(data.getData(), type);
        //4.命中判断缓存是否过期未过期,返回
        if(data.getExpireTime() != null && data.getExpireTime().isAfter(LocalDateTime.now())){
            return result;
        }
        //5.过期则尝试获取互斥锁对象
        boolean lock = tryLock(String.valueOf(id));
        //6.获取成功则开启当独线程访问数据库并修改缓存
        if(lock){
            CACHE_THREAD_POOL.submit(()->{
                try {
                    //1.查询数据库
                    R r  = dbFallback.apply(id);
                    //2.缓存逻辑过期时间
                    setWithLogicalExpire(prefix+id,r,expireTime,unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    unLock(String.valueOf(id));
                }
            });
        }
        //7.默认返回
        return result;
    }
    /**
     * 互斥锁解决缓存击穿问题
     * @param prefix
     * @param id
     * @param type
     * @param expireTime
     * @param unit
     * @param dbFallback
     * @param
     * @param <ID>
     * @return
     */
    public <R,ID> R queryWithMiddleLock(String prefix,ID id,Class<R> type,
                                    Long expireTime,TimeUnit unit,Function<ID, R> dbFallback) {
        //1.查询redis是否存在店铺信息
        String sp = stringRedisTemplate.opsForValue().get(prefix + id);
        //2.存在返回
        if(StrUtil.isNotBlank(sp)){
            return JSONUtil.toBean(sp,type);
        }
        //3.判断查询到的数据是否为null(获取到空缓存的情况)
        if(sp != null){
            return null;
        }
        //4.1获得互斥锁对象
        boolean isLock = tryLock(String.valueOf(id));
        //4.2获取失败则休眠(递归等待)
        R goods;
        try {
            while (!isLock){
                Thread.sleep(50);
                isLock = tryLock(String.valueOf(id));
            }
            sp = stringRedisTemplate.opsForValue().get(prefix + id);
            if(StrUtil.isNotBlank(sp)){
                return JSONUtil.toBean(sp, type);
            }
            //4.3不存在，查询数据库
            goods = dbFallback.apply(id);
            //5.店铺不存在
            if(goods == null) {
                //将空值写入redis
                stringRedisTemplate.opsForValue()
                        .set(prefix+id,"",expireTime,unit);
                return null;
            }
            //6.将查询到的id信息保存到redis
            stringRedisTemplate.opsForValue()
                    .set(prefix + id, JSONUtil.toJsonStr(goods), expireTime,unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //7.释放锁
            unLock(String.valueOf(id));
        }
        //8.返回
        return goods;
    }

}