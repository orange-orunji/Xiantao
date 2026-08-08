package com.xiantao.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiantao.dto.Result;
import com.xiantao.dto.UserDTO;
import com.xiantao.entity.GoodsNote;
import com.xiantao.service.IGoodsNoteService;
import com.xiantao.utils.SystemConstants;
import com.xiantao.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

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
@RequestMapping("/note")
public class GoodsNoteController {

    @Resource
    private IGoodsNoteService noteService;

    @PostMapping
    public Result saveGoodsNote(@RequestBody GoodsNote note) {
        return noteService.saveGoodsNote(note);
    }

    @PutMapping("/like/{id}")
    public Result likeGoodsNote(@PathVariable Long id) {
        // 修改点赞数量；service 已封装 Result（成功/失败），直接透传避免双重嵌套
        return noteService.isLiked(id);
    }

    @GetMapping("/of/me")
    public Result queryMyGoodsNote(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<GoodsNote> page = noteService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<GoodsNote> records = page.getRecords();
        return Result.ok(records);
    }

    @GetMapping("/hot")
    public Result queryHotGoodsNote(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return Result.ok(noteService.queryHotGoodsNotes(current));
    }

    /**
     * 查询博客详情
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result queryGoodsNoteById(@PathVariable Long id){
        return Result.ok(noteService.queryGoodsNote(id));
    }

    /**
     * 查询点赞排行榜
     * @param id
     * @return
     */
    @GetMapping("/likes/{id}")
    public Result queryGoodsNoteLikes(@PathVariable Long id){
        return noteService.queryGoodsNoteLikes(id);
    }

    /**
     * 查询用户博客
     * @param current
     * @param id
     * @return
     */
    @GetMapping("/of/user")
    public Result queryGoodsNoteByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "id", defaultValue = "") Long id
    ){
        Page<GoodsNote> page = noteService.query()
                .eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<GoodsNote> records = page.getRecords();
        return Result.ok(records);
    }

    /**
     * 查询用户关注
     * @param max
     * @param offset
     * @return
     */
    @GetMapping("/of/follow")
    public Result queryGoodsNoteOfFollow(
            @RequestParam("lastId") Long max,@RequestParam(name = "offset", defaultValue = "0") Long offset
    ){
        return noteService.queryGoodsNoteOfFollow(max,offset);
    }
}
