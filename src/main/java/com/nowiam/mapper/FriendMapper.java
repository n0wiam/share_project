package com.nowiam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nowiam.model.pojo.FriendCon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
@Mapper
public interface FriendMapper extends BaseMapper<FriendCon> {
    public List<Integer> myFriend(@Param("userId") Integer userId);
    public List<Integer> friendReq(@Param("userId") Integer userId);

    public void deleteFriend(@Param("friendCon") FriendCon friendCon);
}
