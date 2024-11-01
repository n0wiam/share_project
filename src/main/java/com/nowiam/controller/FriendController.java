package com.nowiam.controller;

import com.nowiam.model.Result;
import com.nowiam.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/friend")
public class FriendController {
    @Autowired
    FriendService friendService;
    @GetMapping
    public Result myFriend(){
        return friendService.myFriend();
    }

    @GetMapping("/req")
    public Result friendReq(){
        return friendService.friendReq();
    }

    @GetMapping("/add/{id}")
    public Result friendAdd(@PathVariable("id") Integer friendId){
        return friendService.friendAdd(friendId);
    }

    @GetMapping("/reply/{id}")
    public Result friendReply(@PathVariable("id") Integer friendId){
        return friendService.friendReply(friendId);
    }

    @GetMapping("/delete/{id}")
    public Result friendDel(@PathVariable("id") Integer friendId){
        return friendService.friendDel(friendId);
    }
}
