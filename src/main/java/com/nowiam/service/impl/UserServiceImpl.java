package com.nowiam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Autowired
    UserMapper userMapper;

    @Autowired
    SignInfoMapper signInfoMapper;

    private final Integer AWARD_DAYS=5;
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
        //user的id由mybatisplus自动回填
        //String token=AppJwtUtil.getToken(user.getId().longValue());
        //Map map=new HashMap<>();
        //map.put("token",token);
        //return new Result<>().ok(map);
        return new Result<>().ok(1);
    }

    @Override
    public Result login(LoginDto loginDto) {
        if(loginDto==null||loginDto.getName()==null||loginDto.getPassword()==null) {return new Result<>().error(400,"登陆失败");}
        User user=userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getName,loginDto.getName()));
        if(user==null) return new Result<>().error(400,"用户不存在");
        if(user.getPassword().equals(loginDto.getPassword()))
        {
//            String token= AppJwtUtil.getToken(user.getId().longValue());
//            Map map=new HashMap<>();
//            map.put("token",token);
//            return new Result<>().ok(map);
            return new Result<>().ok(1);
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

        int today=Integer.parseInt(date.substring(date.lastIndexOf(':')+1));

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
