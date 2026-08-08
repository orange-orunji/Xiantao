package com.xiantao.service;

import com.xiantao.dto.Result;
import com.xiantao.entity.Message;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IMessageService extends IService<Message> {

    Result sendMessage(Long receiverId, String content);

    Result queryChatHistory(Long targetUserId);

    Result queryConversations();

    Result queryUnreadCount();

    Result readAll();

}
