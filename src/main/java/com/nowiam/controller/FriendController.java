package com.nowiam.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nowiam.model.Result;
import com.nowiam.model.pojo.User;
import com.nowiam.service.FriendService;
import com.nowiam.util.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/friend")
public class FriendController {
    @Autowired
    FriendService friendService;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    Gson gson;

    private static final String ACC_KEY="FRIEND_LIST:ACC:";
    private static final String REQ_KEY="FRIEND_LIST:REQ:";
    @GetMapping
    public Result myFriend(){
        List<User> list=null;
        //获取缓存
        list=getCache(ACC_KEY+ ThreadLocalUtil.getUser().getId());
        if(list!=null) return new Result<>().ok(list);
        //读取数据
        list= friendService.myFriend();
        return new Result<>().ok(list);
    }

    @GetMapping("/req")
    public Result friendReq(){
        List<User> list=null;
        //获取缓存
        list=getCache(REQ_KEY+ ThreadLocalUtil.getUser().getId());
        if(list!=null) return new Result<>().ok(list);
        //读取数据
        list = friendService.friendReq();
        return new Result<>().ok(list);
    }

    @GetMapping("/add/{id}")
    public Result friendAdd(@PathVariable("id") Integer friendId){
        //删除对方缓存
        clearCache(REQ_KEY+friendId);
        String res = friendService.friendAdd(friendId);
        return new Result<>().ok(res);
    }

    @GetMapping("/reply/{id}")
    public Result friendReply(@PathVariable("id") Integer friendId){
        //删除双方缓存
        clearCache(REQ_KEY+friendId);
        clearCache(REQ_KEY+ThreadLocalUtil.getUser().getId());
        clearCache(ACC_KEY+ThreadLocalUtil.getUser().getId());
        clearCache(ACC_KEY+friendId);
        String res = friendService.friendReply(friendId);
        return new Result<>().ok(res);
    }

    @GetMapping("/delete/{id}")
    public Result friendDel(@PathVariable("id") Integer friendId){
        //删除双方缓存
        clearCache(REQ_KEY+friendId);
        clearCache(REQ_KEY+ThreadLocalUtil.getUser().getId());
        clearCache(ACC_KEY+ThreadLocalUtil.getUser().getId());
        clearCache(ACC_KEY+friendId);
        String res = friendService.friendDel(friendId);
        return new Result<>().ok(res);
    }

//缓存方法
    private List<User> getCache(String key){
        String str=stringRedisTemplate.opsForValue().get(key);
        List<User> list = gson.fromJson(str, new TypeToken<List<User>>() {
        }.getType());

        if(list==null||list.isEmpty()) return null;

        return list;
    }
    private void clearCache(String key)
    {
        stringRedisTemplate.opsForValue().getOperations().delete(key);
    }
}
