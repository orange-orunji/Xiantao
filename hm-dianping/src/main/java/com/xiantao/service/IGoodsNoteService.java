package com.xiantao.service;

import com.xiantao.dto.Result;
import com.xiantao.entity.GoodsNote;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IGoodsNoteService extends IService<GoodsNote> {

    /**
     * 查询最热博客
     * @param current
     * @return
     */
    List<GoodsNote> queryHotGoodsNotes(Integer current);

    /**
     * 查询博客详情
     * @param id
     * @return
     */
    Object queryGoodsNote(Long id);

    /**
     * 查询是否点赞
     * @param id
     * @return
     */
    Result isLiked(Long id);

    /**
     * 查询点赞
     * @param id
     * @return
     */
    Result queryGoodsNoteLikes(Long id);

    /**
     * 保存博客
     * @param note
     * @return
     */
    Result saveGoodsNote(GoodsNote note);

    /**
     * 查询关注用户
     * @return
     */
    Result queryGoodsNoteOfFollow(Long max, Long offset);
}
