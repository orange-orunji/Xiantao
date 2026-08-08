package com.xiantao.service;

import com.xiantao.dto.Result;
import com.xiantao.entity.Notification;
import com.baomidou.mybatisplus.extension.service.IService;

public interface INotificationService extends IService<Notification> {

    Result queryNotifications();

    Result readAll();
}
