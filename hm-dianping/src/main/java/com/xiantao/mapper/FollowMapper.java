package com.xiantao.mapper;

import com.xiantao.entity.Follow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;

public interface FollowMapper extends BaseMapper<Follow> {

    @Insert("insert into tb_follow values (null,#{userId}, #{followId},#{createTime})")
    void save(Follow follow);
}
