package com.xiantao.service;

import com.xiantao.dto.Result;
import com.xiantao.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IUserInfoService extends IService<UserInfo> {

    Result updateUserInfo(UserInfo userInfo);
}
