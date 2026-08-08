package com.xiantao.service.impl;

import com.xiantao.dto.Result;
import com.xiantao.dto.UserDTO;
import com.xiantao.entity.GoodsNote;
import com.xiantao.entity.NoteComment;
import com.xiantao.entity.User;
import com.xiantao.entity.Notification;
import com.xiantao.mapper.NoteCommentMapper;
import com.xiantao.service.IGoodsNoteService;
import com.xiantao.service.INotificationService;

import com.xiantao.service.INoteCommentService;
import com.xiantao.service.IUserService;
import com.xiantao.utils.UserHolder;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NoteCommentServiceImpl extends ServiceImpl<NoteCommentMapper, NoteComment> implements INoteCommentService {

    @Resource
    private IUserService userService;
    @Resource
    private IGoodsNoteService noteService;
    @Resource
    private INotificationService notificationService;

    @Override
    public Result saveComment(NoteComment comment) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        comment.setUserId(user.getId());
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        comment.setLiked(0);
        comment.setStatus(false);
        save(comment);
        GoodsNote note = noteService.getById(comment.getNoteId());
        if (note != null && !note.getUserId().equals(user.getId())) {
            Notification n = new Notification();
            n.setUserId(note.getUserId());
            n.setFromUserId(user.getId());
            n.setType(2);
            n.setRelatedId(comment.getNoteId());
            n.setContent("评论了你的笔记");
            n.setIsRead(false);
            n.setCreateTime(LocalDateTime.now());
            notificationService.save(n);
        }
        return Result.ok(comment.getId());
    }

    @Override
    public Result queryCommentsByNoteId(Long noteId) {
        List<NoteComment> comments = query()
                .eq("note_id", noteId)
                .eq("parent_id", 0)
                .orderByAsc("create_time")
                .list();
        List<Map<String, Object>> result = new ArrayList<>();
        for (NoteComment comment : comments) {
            User user = userService.getById(comment.getUserId());
            Map<String, Object> map = new HashMap<>();
            map.put("id", comment.getId());
            map.put("content", comment.getContent());
            map.put("liked", comment.getLiked());
            map.put("createTime", comment.getCreateTime());
            map.put("userName", user != null ? user.getNickName() : "匿名用户");
            map.put("userIcon", user != null ? user.getIcon() : "/imgs/icons/default-icon.png");
            result.add(map);
        }
        return Result.ok(result);
    }
}
