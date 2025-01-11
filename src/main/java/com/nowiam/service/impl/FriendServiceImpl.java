package com.nowiam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.gson.Gson;
import com.nowiam.annotation.AutoCache;
import com.nowiam.mapper.FriendMapper;
import com.nowiam.mapper.UserMapper;
import com.nowiam.model.pojo.FriendCon;
import com.nowiam.model.pojo.User;
import com.nowiam.service.FriendService;
import com.nowiam.util.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
    @Autowired
    Gson gson;

    @Override
    @AutoCache("FRIEND_LIST:ACC")
    public List<User> myFriend() {
        Integer userId= ThreadLocalUtil.getUser().getId();

        //读取数据库
        List<Integer> list=friendMapper.myFriend(userId);
        List<User> res=getFriend(list);

        return res;
    }

    @Override
    @AutoCache("FRIEND_LIST:REQ")
    public List<User> friendReq() {
        Integer userId= ThreadLocalUtil.getUser().getId();

        List<Integer> list=friendMapper.friendReq(userId);
        List<User> res=getFriend(list);

        return res;
    }

    @Override
    public String friendAdd(Integer friendId) {
        User friend = userMapper.selectById(friendId);
        Integer userId=ThreadLocalUtil.getUser().getId();
        if(userId.equals(friendId)) return "不能添加自己";
        if(friend==null)
        {
            return "用户不存在";
        }
        FriendCon friendCon = friendMapper.selectOne(Wrappers.<FriendCon>lambdaQuery().eq(FriendCon::getUserId, friendId).eq(FriendCon::getFriendId, userId));
        if(friendCon!=null)
        {
            return "用户已添加";
        }
        //向对方朋友表塞入未确认的关系
        friendCon=new FriendCon();
        friendCon.setFriendId(userId);
        friendCon.setUserId(friendId);
        friendCon.setStatus(0);
        friendMapper.insert(friendCon);


        return "请求发送成功";
    }

    @Override
    public String friendReply(Integer friendId) {
        Integer userId=ThreadLocalUtil.getUser().getId();
        FriendCon friendCon=friendMapper.selectOne(Wrappers.<FriendCon>lambdaQuery().eq(FriendCon::getUserId, userId).eq(FriendCon::getFriendId, friendId));
        if(friendCon!=null){
            friendCon.setStatus(1);
            friendMapper.updateById(friendCon);
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
        }

        return "添加成功";
    }

    @Override
    public String friendDel(Integer friendId) {
        FriendCon friendCon=new FriendCon();
        friendCon.setUserId(ThreadLocalUtil.getUser().getId());
        friendCon.setFriendId(friendId);
        friendMapper.deleteFriend(friendCon);
        return "删除成功";
    }

    private List<User> getFriend(List<Integer> list)
    {
        List<User> res=new ArrayList<>();
        if(list!=null){
            for(Integer i:list)
            {
                User friend=userMapper.selectById(i);
                friend.setPassword(null);
                res.add(friend);
            }
        }
        return res;
    }

}
