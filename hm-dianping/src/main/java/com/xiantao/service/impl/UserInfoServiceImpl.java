package com.xiantao.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xiantao.dto.Result;
import com.xiantao.dto.UserDTO;
import com.xiantao.entity.UserInfo;
import com.xiantao.mapper.UserInfoMapper;
import com.xiantao.service.IUserInfoService;
import com.xiantao.utils.UserHolder;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;


@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

    @Override
    public Result updateUserInfo(UserInfo userInfo) {
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null) {
            return Result.fail("请先登录");
        }
        Long userId = currentUser.getId();
        userInfo.setUserId(userId);
        UserInfo exist = getById(userId);
        if (exist == null) {
            save(userInfo);
        } else {
            UpdateWrapper<UserInfo> wrapper = new UpdateWrapper<>();
            wrapper.eq("user_id", userId);
            if (userInfo.getIntroduce() != null) wrapper.set("introduce", userInfo.getIntroduce());
            if (userInfo.getCity() != null) wrapper.set("city", userInfo.getCity());
            if (userInfo.getGender() != null) wrapper.set("gender", userInfo.getGender());
            if (userInfo.getBirthday() != null) wrapper.set("birthday", userInfo.getBirthday());
            update(wrapper);
        }
        return Result.ok();
    }
}
