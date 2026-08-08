package com.xiantao.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiantao.dto.Result;
import com.xiantao.dto.UserDTO;
import com.xiantao.entity.Goods;
import com.xiantao.entity.GoodsWant;
import com.xiantao.entity.User;
import com.xiantao.mapper.GoodsMapper;
import com.xiantao.mapper.GoodsWantMapper;
import com.xiantao.service.IGoodsService;
import com.xiantao.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiantao.utils.CacheClient;
import com.xiantao.utils.RedisConstants;
import com.xiantao.utils.RedisData;
import com.xiantao.utils.SystemConstants;
import com.xiantao.utils.UserHolder;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.xiantao.utils.RedisConstants.*;

@Service
@Slf4j
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements IGoodsService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;
    @Resource
    private IUserService userService;
    @Resource
    private GoodsWantMapper goodsWantMapper;
    /**
     * 根据id查询商品信息
     * @param id 商品id
     * @return 商品信息
     */
    @Override
    public Result queryById(Long id) {
        //缓存穿透
//        Goods goods = queryWithPassThrough(id);
        //互斥锁解决缓存击穿(缓存穿透解决(保存空缓存)模板上加上了缓存击穿)
//        Goods goods = queryWithMutex(id);
        //逻辑锁解决缓存击穿问题
        Goods goods = cacheClient.queryWithExpireTime(CACHE_GOODS_KEY,id,Goods.class,30L,TimeUnit.MINUTES,this::getById);
        if(goods == null){
            // 缓存未命中（未预热/缓存丢失）时回源查库，避免商品详情查不到
            goods = getById(id);
        }
        if(goods == null){
            return Result.fail("商品不存在");
        }
        // 记录浏览足迹（登录用户）
        recordBrowse(id);
        //返回
        return Result.ok(goods);
    }

    /**
     * 逻辑锁解决缓存击穿
     * @param id
     * @return
     */

    public Goods queryWithMutex(Long id) {
        //1.查询redis是否存在店铺信息
        String sp = stringRedisTemplate.opsForValue().get(CACHE_GOODS_KEY + id);
        //2.存在返回
        if(StrUtil.isNotBlank(sp)){
            return JSONUtil.toBean(sp, Goods.class);
        }
        //3.判断查询到的数据是否为null(获取到空缓存的情况)
        if(sp != null){
            return null;
        }
        //4.1获得互斥锁对象
        boolean isLock = tryLock(id);
        //4.2获取失败则休眠(递归等待)
        Goods goods;
        try {
            while (!isLock){
                Thread.sleep(50);
                isLock = tryLock(id);
            }
            sp = stringRedisTemplate.opsForValue().get(CACHE_GOODS_KEY + id);
            if(StrUtil.isNotBlank(sp)){
                return JSONUtil.toBean(sp, Goods.class);
            }
            //4.3不存在，查询数据库
            goods = getById(id);
            //5.店铺不存在
            if(goods == null) {
                //将空值写入redis
                stringRedisTemplate.opsForValue()
                        .set(CACHE_GOODS_KEY+id,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
                return null;
            }
            //6.将查询到的id信息保存到redis
            stringRedisTemplate.opsForValue()
                    .set(CACHE_GOODS_KEY + id, JSONUtil.toJsonStr(goods), CACHE_GOODS_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //7.释放锁
            unLock(id);
        }
        //8.返回
        return goods;
    }
    /**
     * 缓存穿透解决方法
     * @param id
     * @return
     */
    public Goods queryWithPassThrough(Long id) {
        //查询redis是否存在店铺信息
        String sp = stringRedisTemplate.opsForValue().get(CACHE_GOODS_KEY + id);
        //存在返回
        if(StrUtil.isNotBlank(sp)){
            return JSONUtil.toBean(sp, Goods.class);
        }
        //判断查询到的数据是否为null
        if(sp != null){
            return null;
        }
        //不存在，查询数据库
        Goods goods = getById(id);
        //店铺不存在
        if(goods == null) {
            //设置空缓存
            stringRedisTemplate.opsForValue()
                    .set(CACHE_GOODS_KEY+id,"", CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }
        //保存到redis
        stringRedisTemplate.opsForValue().set(CACHE_GOODS_KEY + id, JSONUtil.toJsonStr(goods), CACHE_GOODS_TTL, TimeUnit.MINUTES);
        return goods;
    }

    /**
     * 逻辑锁解决缓存击穿
     * @param id
     * @return
     */
    private boolean tryLock(Long id){
        return BooleanUtil.isTrue(stringRedisTemplate.opsForValue().setIfAbsent(
                LOCK_GOODS_KEY+id,
                "",
                RedisConstants.LOCK_GOODS_TTL,
                TimeUnit.SECONDS
        ));
    }

    private boolean unLock(Long id){
        return BooleanUtil.isTrue(stringRedisTemplate.delete(LOCK_GOODS_KEY+id));
    }

    //获取线程池
    private static final ExecutorService CACHE_THREAD_POOL= Executors.newFixedThreadPool(10);
    /**
     * 逻辑锁解决缓存击穿问题
     * @param id
     * @return
     */
    public Goods queryWithExpireTime(Long id) {
        //1.查询redis是否存在店铺信息
        String sp = stringRedisTemplate.opsForValue().get(CACHE_GOODS_KEY + id);
        //2.不存在返回,说明不是热门商品
        if(StrUtil.isBlank(sp)){
            return null;
        }
        //3.数据不为空
        RedisData data = BeanUtil.toBean(sp, RedisData.class);
        Goods goods = BeanUtil.toBean(data.getData(), Goods.class);
        //4.命中判断缓存是否过期未过期,返回
        if(data.getExpireTime() != null && data.getExpireTime().isAfter(LocalDateTime.now())){
            return goods;
        }
        //5.过期则尝试获取互斥锁对象
        boolean lock = tryLock(id);
        //6.获取成功则开启当独线程访问数据库并修改缓存
        if(lock){
            CACHE_THREAD_POOL.submit(()->{
                try {
                    saveGoods2Redis(id,30L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    unLock(id);
                }
            });
        }
        //7.默认返回
        return goods;
    }
    public void saveGoods2Redis(Long id,Long expireSeconds) {
        Goods goods = getById(id);
        RedisData redisData = new RedisData();
        redisData.setData(goods);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        stringRedisTemplate.opsForValue().set(CACHE_GOODS_KEY+id,JSONUtil.toJsonStr(redisData));
    }

    /**
     * 启动预热：将全部在售商品写入逻辑过期缓存
     * 逻辑过期方案（queryWithExpireTime）缓存未命中时直接返回 null，必须预热才能保证详情可查
     */
    @PostConstruct
    public void preloadGoodsCache() {
        try {
            List<Goods> list = query().eq("status", 1).list();
            list.forEach(g -> saveGoods2Redis(g.getId(), 30L));
            log.info("商品缓存预热完成，共 {} 条", list.size());
        } catch (Exception e) {
            // Redis 未启动等场景不应阻断应用启动，降级为每次查询回源数据库
            log.warn("商品缓存预热失败（Redis 不可用？），详情接口将回源数据库：{}", e.getMessage());
        }
    }

    /**
     * 发布商品
     * @param goods 商品数据
     * @return 商品id
     */
    @Override
    public Result saveGoods(Goods goods) {
        // 1. 校验关键信息
        if (StrUtil.isBlank(goods.getName()) || goods.getPrice() == null || goods.getPrice() <= 0) {
            return Result.fail("商品名称和价格不能为空");
        }
        // 2. 获取登录用户作为卖家
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        goods.setSellerId(user.getId());
        // 3. 初始化状态与统计
        goods.setStatus(1);
        goods.setSold(0);
        goods.setComments(0);
        // 4. 保存商品
        boolean success = save(goods);
        if (!success) {
            return Result.fail("发布失败");
        }
        return Result.ok(goods.getId());
    }

    /**
     * 想要/取消想要商品（ZSet 记录想要用户，DB 维护想要数量）
     * @param id 商品id
     * @return 结果
     */
    @Override
    public Result wantGoods(Long id) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        String userId = user.getId().toString();
        String key = RedisConstants.GOODS_WANT_KEY + id;
        // 已想要则取消，未想要则加入
        Double score = stringRedisTemplate.opsForZSet().score(key, userId);
        if (score == null) {
            boolean b = update().setSql("comments = comments + 1").eq("id", id).update();
            if (b) {
                stringRedisTemplate.opsForZSet().add(key, userId, System.currentTimeMillis());
                // 双写：记录到 MySQL，供"我的想要"列表查询（唯一键防重）
                GoodsWant want = new GoodsWant();
                want.setUserId(user.getId());
                want.setGoodsId(id);
                want.setCreateTime(LocalDateTime.now());
                try {
                    goodsWantMapper.insert(want);
                } catch (DuplicateKeyException e) {
                    // 已存在记录，忽略（数据一致性兜底）
                }
            }
        } else {
            boolean b = update().setSql("comments = comments - 1").eq("id", id).update();
            if (b) {
                stringRedisTemplate.opsForZSet().remove(key, userId);
                // 双写：删除 MySQL 记录（注意：JDK17 下不能用 LambdaQueryWrapper，MyBatis-Plus 3.4.3 反射解析会报 InaccessibleObjectException）
                goodsWantMapper.delete(new QueryWrapper<GoodsWant>()
                        .eq("user_id", user.getId())
                        .eq("goods_id", id));
            }
        }
        return Result.ok(true);
    }

    /**
     * 查询当前用户想要的商品列表（按想要时间倒序）
     * @return 商品列表（含想要时间）
     */
    @Override
    public Result queryMyWants() {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        List<GoodsWant> wants = goodsWantMapper.selectList(new QueryWrapper<GoodsWant>()
                .eq("user_id", user.getId())
                .orderByDesc("create_time"));
        if (wants == null || wants.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> goodsIds = wants.stream().map(GoodsWant::getGoodsId).collect(Collectors.toList());
        List<Goods> goodsList = query().in("id", goodsIds)
                .last("order by field(id," + StringUtil.join(goodsIds, ",") + ")").list();
        Map<Long, Goods> goodsMap = goodsList.stream()
                .collect(Collectors.toMap(Goods::getId, g -> g));
        List<Map<String, Object>> result = wants.stream().map(w -> {
            Map<String, Object> item = new HashMap<>();
            item.put("wantTime", w.getCreateTime());
            item.put("goods", goodsMap.get(w.getGoodsId()));
            return item;
        }).collect(Collectors.toList());
        return Result.ok(result);
    }

    /**
     * 查询当前用户发布的商品列表
     * @return 商品列表
     */
    @Override
    public Result queryMyGoods() {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        List<Goods> goodsList = query().eq("seller_id", user.getId())
                .orderByDesc("update_time").list();
        return Result.ok(goodsList);
    }

    /**
     * 记录商品浏览足迹（登录用户，ZSet 按时间排序，仅保留最近 50 条）
     * @param id 商品id
     */
    @Override
    public void recordBrowse(Long id) {
        UserDTO user = UserHolder.getUser();
        if (user == null || id == null) {
            return;
        }
        String key = RedisConstants.BROWSE_GOODS_KEY + user.getId();
        stringRedisTemplate.opsForZSet().add(key, id.toString(), System.currentTimeMillis());
        // 只保留最近 50 条
        stringRedisTemplate.opsForZSet().removeRange(key, 0, -51);
    }

    /**
     * 查询当前用户最近浏览的商品列表（最多 20 条）
     * @return 商品列表
     */
    @Override
    public Result queryBrowseList() {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        String key = RedisConstants.BROWSE_GOODS_KEY + user.getId();
        Set<String> ids = stringRedisTemplate.opsForZSet().reverseRange(key, 0, 19);
        if (ids == null || ids.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> goodsIds = ids.stream().map(Long::parseLong).collect(Collectors.toList());
        List<Goods> goodsList = query().in("id", goodsIds)
                .last("order by field(id," + StringUtil.join(goodsIds, ",") + ")").list();
        return Result.ok(goodsList);
    }

    /**
     * 查询想要该商品的前5名用户（按想要时间排序）
     * @param id 商品id
     * @return 用户列表与当前用户是否想要
     */
    @Override
    public Result queryGoodsWants(Long id) {
        String key = RedisConstants.GOODS_WANT_KEY + id;
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        // 当前登录用户是否已想要
        boolean wanted = false;
        UserDTO current = UserHolder.getUser();
        if (current != null) {
            wanted = stringRedisTemplate.opsForZSet().score(key, current.getId().toString()) != null;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("wanted", wanted);
        if (top5 == null || top5.isEmpty()) {
            result.put("users", Collections.emptyList());
            return Result.ok(result);
        }
        List<Long> ids = top5.stream().map(Long::parseLong).collect(Collectors.toList());
        String linkedId = StringUtil.join(ids, ",");
        List<User> users = userService.query().select("id", "nick_name", "icon").in("id", ids)
                .last("order by field(id," + linkedId + ")").list();
        List<UserDTO> collect = users.stream().map(u -> {
            UserDTO userDTO = new UserDTO();
            BeanUtils.copyProperties(u, userDTO);
            return userDTO;
        }).collect(Collectors.toList());
        result.put("users", collect);
        return Result.ok(result);
    }

    /**
     * 更新商品信息
     * @param goods 商品信息
     * @return 无
     */
    @Transactional
    @Override
    public void updateGoods(Goods goods) {
        // 更新数据库
        updateById(goods);
        // 删除缓存
        stringRedisTemplate.delete(CACHE_GOODS_KEY+goods.getId());
    }

    @Override
    public Result queryByPage(Integer typeId, Integer current, double x, double y) {
        //1.判断是否需要按坐标为null则默认查询
        if(x == 0.0d && y == 0.0d){
            Page<Goods> page = query()
                    .eq(typeId != null && typeId > 0, "type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }
        //2.获取分页参数
        //2.1.获取起始页数后续便于截取
        Integer startIndex = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        //2.2 定义要截取的店铺数量
        Integer size = current* SystemConstants.DEFAULT_PAGE_SIZE;
        String key = RedisConstants.GOODS_GEO_KEY + typeId;
        //3.查询redis中对于的商店信息
        GeoResults<RedisGeoCommands.GeoLocation<String>> geos = stringRedisTemplate.opsForGeo().search(
                key,
                GeoReference.fromCoordinate(x, y),
                new Distance(5000),//5000米单位默认米
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeCoordinates().includeDistance().limit(size)
        );
        //GEO数据不存在或为空，fallback到普通数据库查询
        if(geos == null || geos.getContent().isEmpty()){
            Page<Goods> page = query()
                    .eq(typeId != null && typeId > 0, "type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }
        //4.解析响应结果出店铺id
        List<Long> ids = new ArrayList<>(size);
        Map<String,Double> map = new HashMap<>(size);
        //非空判断
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> lists = geos.getContent();
        if(lists.size() <= startIndex ) {
            return Result.ok(Collections.emptyList());
        }
        //跳过起始的页数
        lists.stream().skip(startIndex).forEach(item -> {
            String id = item.getContent().getName();
            ids.add(Long.parseLong(id));
            map.put(id,item.getDistance().getValue());
        });
        //5.批量查询数据库
        List<Goods> result = query().in("id", ids)
                .last("order by field(id," + StringUtil.join(ids, ",") + ")").list();
        for (Goods goods : result) {
            goods.setDistance(map.get(goods.getId().toString()));
        }
        //6.返回
        return Result.ok(result);
    }
}
