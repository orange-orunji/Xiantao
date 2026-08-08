package com.xiantao.controller;

import com.xiantao.dto.Result;
import com.xiantao.service.INotificationService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Resource
    private INotificationService notificationService;

    @GetMapping
    public Result queryNotifications() {
        return notificationService.queryNotifications();
    }

    @PutMapping("/read")
    public Result readAll() {
        return notificationService.readAll();
    }
}
