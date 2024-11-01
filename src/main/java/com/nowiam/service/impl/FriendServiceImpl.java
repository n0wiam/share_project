package com.nowiam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nowiam.mapper.FriendMapper;
import com.nowiam.mapper.UserMapper;
import com.nowiam.model.Result;
import com.nowiam.model.pojo.FriendCon;
import com.nowiam.model.pojo.User;
import com.nowiam.service.FriendService;
import com.nowiam.util.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FriendServiceImpl implements FriendService {
    @Autowired
    FriendMapper friendMapper;
    @Autowired
    UserMapper userMapper;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private static final String KEY="FRIEND_LIST:";

    @Override
    public Result myFriend() {
        Integer userId= ThreadLocalUtil.getUser().getId();
        List<Integer> list=friendMapper.myFriend(userId);
        //缓存
        return new Result<>().ok(getFriend(list));
    }

    @Override
    public Result friendReq() {
        Integer userId= ThreadLocalUtil.getUser().getId();
        List<Integer> list=friendMapper.friendReq(userId);
        //缓存
        return new Result<>().ok(getFriend(list));
    }

    @Override
    public Result friendAdd(Integer friendId) {
        User friend = userMapper.selectById(friendId);
        Integer userId=ThreadLocalUtil.getUser().getId();
        if(friend==null)
        {
            return new Result<>().ok("用户不存在");
        }
        FriendCon friendCon = friendMapper.selectOne(Wrappers.<FriendCon>lambdaQuery().eq(FriendCon::getUserId, friendId).eq(FriendCon::getFriendId, userId));
        if(friendCon!=null)
        {
            return new Result<>().ok("用户已添加");
        }
        //向对方朋友表塞入未确认的关系
        friendCon=new FriendCon();
        friendCon.setFriendId(userId);
        friendCon.setUserId(friendId);
        friendCon.setStatus(0);
        friendMapper.insert(friendCon);

        //TODO:清对方缓存


        return new Result<>().ok("请求发送成功");
    }

    @Override
    public Result friendReply(Integer friendId) {
        Integer userId=ThreadLocalUtil.getUser().getId();
        FriendCon friendCon=friendMapper.selectOne(Wrappers.<FriendCon>lambdaQuery().eq(FriendCon::getUserId, userId).eq(FriendCon::getFriendId, friendId));
        if(friendCon!=null){
            friendCon.setStatus(1);
            friendMapper.updateById(friendCon);
        }
        //确认关系
        FriendCon friendCon1=friendMapper.selectOne(Wrappers.<FriendCon>lambdaQuery().eq(FriendCon::getUserId, friendId).eq(FriendCon::getFriendId, userId));
        if(friendCon1==null) {
            friendCon1=new FriendCon();
            friendCon1.setUserId(friendId);
            friendCon1.setFriendId(userId);
            friendCon1.setStatus(1);
            friendMapper.insert(friendCon1);
        }else{
            friendCon1.setStatus(1);
            friendMapper.updateById(friendCon1);
        }

        //TODO:清理双方缓存


        return new Result<>().ok("添加成功");
    }

    @Override
    public Result friendDel(Integer friendId) {
        FriendCon friendCon=new FriendCon();
        friendCon.setUserId(ThreadLocalUtil.getUser().getId());
        friendCon.setFriendId(friendId);
        //TODO:清理双方缓存
        friendMapper.deleteFriend(friendCon);
        return new Result<>().ok("删除成功");
    }

    private List<User> getFriend(List<Integer> list)
    {
        List<User> res=new ArrayList<>();
        for(Integer i:list)
        {
            User friend=userMapper.selectById(i);
            friend.setPassword(null);
            res.add(friend);
        }
        return res;
    }

}
