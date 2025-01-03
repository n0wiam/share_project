package com.nowiam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.nowiam.mapper.SignInfoMapper;
import com.nowiam.mapper.UserMapper;
import com.nowiam.model.Result;
import com.nowiam.model.dto.LoginDto;
import com.nowiam.model.pojo.SignInfo;
import com.nowiam.model.pojo.User;
import com.nowiam.service.UserService;
import com.nowiam.util.ThreadLocalUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Autowired
    UserMapper userMapper;

    @Autowired
    SignInfoMapper signInfoMapper;

    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    Gson gson;

    private final Integer AWARD_DAYS=5;
    @Override
    public Result info() {
        return new Result<>().ok(userMapper.selectById(ThreadLocalUtil.getUser().getId()));
    }
    @Override
    public Result register(LoginDto loginDto) {
        User user=new User();
        user.setCoin(0);
        BeanUtils.copyProperties(loginDto,user);
        try {
            userMapper.insert(user);
        }catch (Exception e) {
            return new Result<>().error(400,"用户已存在");
        }
        return new Result<>().ok("注册成功");
    }

    @Override
    public Result login(LoginDto loginDto) {
        if(loginDto==null||loginDto.getName()==null||loginDto.getPassword()==null) {return new Result<>().error(400,"登陆失败");}
        User user=userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getName,loginDto.getName()));
        if(user==null) return new Result<>().error(400,"用户不存在");
        if(user.getPassword().equals(loginDto.getPassword()))
        {
            user.setPassword("");
            String token= UUID.randomUUID().toString();
            stringRedisTemplate.opsForValue().set("USER:"+token,gson.toJson(user),30, TimeUnit.MINUTES);
            return new Result<>().ok(token);
        }
        return new Result<>().error(400,"登陆失败");
    }

    @Override
    public Result sign() {
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy:MM");
        String date=sdf.format(new Date());
        Integer userID = ThreadLocalUtil.getUser().getId();
        if(userID==null) return new Result<>().error(400,"签到失败");

        SignInfo signInfo = signInfoMapper.selectOne(Wrappers.<SignInfo>lambdaQuery().eq(SignInfo::getUserId, userID).eq(SignInfo::getDate, date));


        int today=LocalDateTime.now().getDayOfMonth();

        if(signInfo==null)
        {
            signInfo=new SignInfo();
            signInfo.setDate(date);
            signInfo.setUserId(userID);
            signInfoMapper.insert(signInfo);
        }

        //签到过的判断
        Long info=signInfo.getInfo();
        if(((info>>today)&1)==1)
        {
            return new Result<>().ok(-1);
        }
        //进入签到
        info|=(1L<<today);
        signInfo.setInfo(info);
        signInfoMapper.updateById(signInfo);
        int  signedDays=0;
        for(int i=1;i<=31;i++)
        {
            if( ( (info>>i)&1 )==1 ) signedDays++;
        }
        //TODO:策略模式
        //签到获得金币
        User user=userMapper.selectById(userID);
        if(signedDays%AWARD_DAYS==0)
        {
            user.setCoin(user.getCoin()+signedDays/AWARD_DAYS);
            userMapper.updateById(user);
        }

        return new Result<>().ok(signedDays);
    }
}
