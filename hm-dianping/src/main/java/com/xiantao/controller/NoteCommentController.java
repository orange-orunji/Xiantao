package com.xiantao.controller;

import com.xiantao.dto.Result;
import com.xiantao.entity.NoteComment;
import com.xiantao.service.INoteCommentService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/note-comments")
public class NoteCommentController {

    @Resource
    private INoteCommentService noteCommentsService;

    @PostMapping
    public Result saveComment(@RequestBody NoteComment comment) {
        return noteCommentsService.saveComment(comment);
    }

    @GetMapping("/note/{id}")
    public Result queryCommentsByNoteId(@PathVariable Long id) {
        return noteCommentsService.queryCommentsByNoteId(id);
    }
}
