package com.xiantao.service.impl;

import com.xiantao.dto.Result;
import com.xiantao.dto.UserDTO;
import com.xiantao.entity.Message;
import com.xiantao.entity.User;
import com.xiantao.mapper.MessageMapper;
import com.xiantao.service.IMessageService;
import com.xiantao.service.IUserService;
import com.xiantao.utils.UserHolder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements IMessageService {

    @Resource
    private IUserService userService;

    @Override
    public Result sendMessage(Long receiverId, String content) {
        UserDTO user = UserHolder.getUser();
        if (user == null) return Result.fail("请先登录");
        if (content == null || content.isBlank()) return Result.fail("内容不能为空");
        Message msg = new Message();
        msg.setSenderId(user.getId());
        msg.setReceiverId(receiverId);
        msg.setContent(content.trim());
        msg.setIsRead(false);
        msg.setCreateTime(LocalDateTime.now());
        save(msg);
        return Result.ok(msg.getId());
    }

    @Override
    public Result queryChatHistory(Long targetUserId) {
        UserDTO user = UserHolder.getUser();
        Long myId = user.getId();
        List<Message> list = query()
                .and(w -> w.eq("sender_id", myId).eq("receiver_id", targetUserId)
                        .or(q -> q.eq("sender_id", targetUserId).eq("receiver_id", myId)))
                .orderByAsc("create_time")
                .list();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Message m : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("senderId", m.getSenderId());
            map.put("receiverId", m.getReceiverId());
            map.put("content", m.getContent());
            map.put("isRead", m.getIsRead());
            map.put("createTime", m.getCreateTime());
            map.put("isMine", m.getSenderId().equals(myId));
            result.add(map);
        }
        update(new UpdateWrapper<Message>().eq("receiver_id", myId).eq("sender_id", targetUserId)
                .eq("is_read", false).set("is_read", true));
        return Result.ok(result);
    }

    @Override
    public Result queryUnreadCount() {
        UserDTO user = UserHolder.getUser();
        if (user == null) return Result.fail("请先登录");
        // 统计所有发给当前用户且未读的消息总数
        long count = count(new QueryWrapper<Message>()
                .eq("receiver_id", user.getId()).eq("is_read", false));
        return Result.ok(count);
    }

    @Override
    public Result readAll() {
        UserDTO user = UserHolder.getUser();
        update(new UpdateWrapper<Message>()
                .eq("receiver_id", user.getId()).eq("is_read", false)
                .set("is_read", true));
        return Result.ok();
    }

    @Override
    public Result queryConversations() {
        UserDTO user = UserHolder.getUser();
        Long myId = user.getId();
        List<Message> all = query()
                .and(w -> w.eq("sender_id", myId).or(q -> q.eq("receiver_id", myId)))
                .orderByDesc("create_time")
                .list();
        Map<Long, Message> latest = new LinkedHashMap<>();
        for (Message m : all) {
            Long otherId = m.getSenderId().equals(myId) ? m.getReceiverId() : m.getSenderId();
            if (!latest.containsKey(otherId)) {
                latest.put(otherId, m);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Message> e : latest.entrySet()) {
            Long otherId = e.getKey();
            User u = userService.getById(otherId);
            int unreadCount = count(new QueryWrapper<Message>()
                    .eq("receiver_id", myId).eq("sender_id", otherId).eq("is_read", false));
            Map<String, Object> map = new HashMap<>();
            map.put("userId", otherId);
            map.put("nickName", u != null ? u.getNickName() : "匿名");
            map.put("icon", u != null ? u.getIcon() : "/imgs/icons/default-icon.png");
            map.put("lastContent", e.getValue().getContent());
            map.put("lastTime", e.getValue().getCreateTime());
            map.put("unreadCount", unreadCount);
            result.add(map);
        }
        return Result.ok(result);
    }
}
