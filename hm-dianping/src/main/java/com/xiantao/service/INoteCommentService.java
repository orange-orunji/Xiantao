package com.xiantao.service;

import com.xiantao.dto.Result;
import com.xiantao.entity.NoteComment;
import com.baomidou.mybatisplus.extension.service.IService;

public interface INoteCommentService extends IService<NoteComment> {

    Result saveComment(NoteComment comment);

    Result queryCommentsByNoteId(Long noteId);
}
