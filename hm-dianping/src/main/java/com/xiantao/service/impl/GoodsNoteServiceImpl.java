package com.xiantao.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiantao.dto.Result;
import com.xiantao.dto.ScrollResult;
import com.xiantao.dto.UserDTO;
import com.xiantao.entity.GoodsNote;
import com.xiantao.entity.Follow;
import com.xiantao.entity.Notification;
import com.xiantao.entity.User;
import com.xiantao.mapper.GoodsNoteMapper;
import com.xiantao.service.IGoodsNoteService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiantao.service.IFollowService;
import com.xiantao.service.INotificationService;
import com.xiantao.service.IUserService;
import com.xiantao.utils.RedisConstants;
import com.xiantao.utils.SystemConstants;
import com.xiantao.utils.UserHolder;
import jodd.util.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GoodsNoteServiceImpl extends ServiceImpl<GoodsNoteMapper, GoodsNote> implements IGoodsNoteService {

    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IFollowService followService;
    @Resource
    private INotificationService notificationService;

    @Override
    public List<GoodsNote> queryHotGoodsNotes(Integer current) {
        // 根据用户查询
        Page<GoodsNote> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<GoodsNote> records = page.getRecords();
        // 查询用户
        records.forEach(note -> {
            this.extracted(note);
            this.isGoodsNoteLiked(note);
        });
        return records;
    }

    private void extracted(GoodsNote note) {
        Long userId = note.getUserId();
        User user = userService.getById(userId);
        note.setName(user.getNickName());
        note.setIcon(user.getIcon());
    }
    private void isGoodsNoteLiked(GoodsNote note){
        UserDTO user = UserHolder.getUser();
        if (user == null){
            note.setIsLike(false);
            return;
        }
        long id = note.getUserId();
        Double score = stringRedisTemplate.opsForZSet().score(RedisConstants.NOTE_LIKED_KEY + id, String.valueOf(id));
        note.setIsLike(score != null);
    }

    @Override
    public Object queryGoodsNote(Long id) {
        //1.根据id查询博客
        GoodsNote note = getById(id);
        //2.根据博客id查询用户相关信息
        extracted(note);
        //3.查询当前用户是否点赞过该博客
        isGoodsNoteLiked(note);
        return note;
    }

    /**
     * 获取当前用户是否点赞过该博客
     * @param id 博客id
     * @return
     */
    public Result isLiked(Long id) {
        String userId = UserHolder.getUser().getId().toString();
        String key = RedisConstants.NOTE_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId);
        if(score == null) {
            boolean b = update().setSql("liked = liked + 1").eq("id", id).update();
            if(b){
                stringRedisTemplate.opsForZSet().add(key, userId, System.currentTimeMillis());
                GoodsNote note = getById(id);
                if (note != null && !note.getUserId().equals(Long.valueOf(userId))) {
                    Notification n = new Notification();
                    n.setUserId(note.getUserId());
                    n.setFromUserId(Long.valueOf(userId));
                    n.setType(1);
                    n.setRelatedId(id);
                    n.setContent("赞了你的笔记");
                    n.setIsRead(false);
                    n.setCreateTime(LocalDateTime.now());
                    notificationService.save(n);
                }
            }
        }
        else {
            boolean b = update().setSql("liked = liked - 1").eq("id", id).update();
            if(b){
                stringRedisTemplate.opsForZSet().remove(key,userId);
            }
        }
        return Result.ok(true);
    }

    /**
     * 查询博客点赞排行榜
     * @param id
     * @return
     */
    @Override
    public Result queryGoodsNoteLikes(Long id) {
        String key = RedisConstants.NOTE_LIKED_KEY + id;
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if(top5 == null || top5.isEmpty()) return Result.ok();
        List<Long> ids = top5.stream().map(Long::parseLong).collect(Collectors.toList());
        String linkedId = StringUtil.join(ids, ",");
        List<User> users = userService.query().select("id", "nick_name", "icon").in("id", ids)
                .last("order by field(id," + linkedId +")").list();
        List<UserDTO> collect = users.stream().map(user -> {
            UserDTO userDTO = new UserDTO();
            BeanUtils.copyProperties(user, userDTO);
            return userDTO;
        }).collect(Collectors.toList());
        return Result.ok(collect);
    }

    @Override
    public Result saveGoodsNote(GoodsNote note) {
        //1. 获取登录用户
        UserDTO user = UserHolder.getUser();
        note.setUserId(user.getId());
        // 2. 保存探店博文
        boolean isSuccess = save(note);
        //3. 保存成功后,查询当前博主的所有粉丝数据
        if(!isSuccess){
            return Result.fail("发布失败");
        }
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        List<Long> ids = follows.stream().map(Follow::getUserId).collect(Collectors.toList());
        //4. 推送数据给粉丝邮箱
        for (Long id : ids) {
            String key1 = RedisConstants.FEED_KEY + id;
            stringRedisTemplate.opsForZSet().add(key1, note.getId().toString(), System.currentTimeMillis());
        }
        // 返回id
        return Result.ok(note.getId());
    }

    @Override
    public Result queryGoodsNoteOfFollow(Long max,Long offset) {
        //1.获取当前用户相关信息
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();
        String key = RedisConstants.FEED_KEY + userId;
        //2.获取当前用户的收件箱
        Set<String> set = stringRedisTemplate.opsForZSet().reverseRangeByScore(key, 0, max, offset.intValue(), 3);
        if(set == null || set.isEmpty())return Result.ok();
        List<Long> ids = new ArrayList<>(set.size());
        long min = 0L;
        int count = 1;
        //3.获取当前用户的收件箱中的id并更行要返回的各个参数
        for (String s : set) {
            long id = Long.parseLong(s);
            ids.add(id);
            if(id == min){
                count++;
            }else{
                min = id;
                count = 1;
            }
        }
        //4.根据id查询博客
        String linkedId = StringUtil.join(ids, ",");
        List<GoodsNote> notes = query().in("id", ids).last("order by field(id, " + linkedId + ")").list();
        //5.定义好博客的内部类
        for (GoodsNote note : notes) {
            //2.根据博客id查询用户相关信息
            extracted(note);
            //3.查询当前用户是否点赞过该博客
            isGoodsNoteLiked(note);
        }

        //6.封装返回
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setOffset(count);
        scrollResult.setList(notes);
        scrollResult.setMinTime(min);

        return Result.ok(scrollResult);
    }
}
