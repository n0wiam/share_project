package com.nowiam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nowiam.model.Result;
import com.nowiam.model.dto.LoginDto;
import com.nowiam.model.pojo.User;

public interface UserService extends IService<User> {
    Result info();
    Result register(LoginDto loginDto);

    Result login(LoginDto loginDto);

    Result sign();
}
