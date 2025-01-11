package com.nowiam.controller;

import com.nowiam.model.dto.LoginDto;
import com.nowiam.model.Result;
import com.nowiam.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;
    @GetMapping
    public void hello(){
        System.out.println(123);
    }
    @GetMapping("/info")
    public Result info(){
        return userService.info();
    }

    @PostMapping("/register")
    public Result register(@RequestBody LoginDto loginDto){
        return userService.register(loginDto);
    }

    @PostMapping("/login")
    public Result login(@RequestBody LoginDto loginDto){
        //System.out.println(loginDto.getUid());
        return userService.login(loginDto);
    }

    @GetMapping("/sign")
    public Result sign()
    {
        return userService.sign();
    }
}
