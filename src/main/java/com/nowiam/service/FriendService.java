package com.nowiam.service;

import com.nowiam.annotation.AutoCaches;
import com.nowiam.model.Result;
import com.nowiam.model.pojo.User;

import java.util.List;

public interface FriendService {
    List<User> myFriend();
    List<User> friendReq();

    String friendAdd(Integer friendId);

    String friendReply(Integer friendId);

    String friendDel(Integer friendId);
}
