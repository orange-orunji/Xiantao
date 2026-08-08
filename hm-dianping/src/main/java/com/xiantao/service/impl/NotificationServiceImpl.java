package com.xiantao.service.impl;

import com.xiantao.dto.Result;
import com.xiantao.dto.UserDTO;
import com.xiantao.entity.Notification;
import com.xiantao.entity.User;
import com.xiantao.mapper.NotificationMapper;
import com.xiantao.service.INotificationService;
import com.xiantao.service.IUserService;
import com.xiantao.utils.UserHolder;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements INotificationService {

    @Resource
    private IUserService userService;

    @Override
    public Result queryNotifications() {
        UserDTO user = UserHolder.getUser();
        List<Notification> list = query()
                .eq("user_id", user.getId())
                .orderByDesc("create_time")
                .last("limit 50")
                .list();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Notification n : list) {
            User from = userService.getById(n.getFromUserId());
            Map<String, Object> m = new HashMap<>();
            m.put("id", n.getId());
            m.put("type", n.getType());
            m.put("content", n.getContent());
            m.put("relatedId", n.getRelatedId());
            m.put("isRead", n.getIsRead());
            m.put("createTime", n.getCreateTime());
            m.put("fromUserName", from != null ? from.getNickName() : "匿名");
            m.put("fromUserIcon", from != null ? from.getIcon() : "/imgs/icons/default-icon.png");
            result.add(m);
        }
        return Result.ok(result);
    }

    @Override
    public Result readAll() {
        UserDTO user = UserHolder.getUser();
        update(new UpdateWrapper<Notification>()
                .eq("user_id", user.getId()).eq("is_read", false)
                .set("is_read", true));
        return Result.ok();
    }
}
