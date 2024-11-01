package com.nowiam.service;

import com.nowiam.model.Result;

public interface FriendService {
    Result myFriend();

    Result friendReq();

    Result friendAdd(Integer friendId);

    Result friendReply(Integer friendId);

    Result friendDel(Integer friendId);
}
