package com.nowiam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nowiam.mapper.ImageConMapper;
import com.nowiam.mapper.ImageMapper;
import com.nowiam.mapper.UserMapper;
import com.nowiam.model.Result;
import com.nowiam.model.pojo.Image;
import com.nowiam.model.pojo.ImageCon;
import com.nowiam.model.pojo.User;
import com.nowiam.service.ImageService;
import com.nowiam.util.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ImageServiceImpl implements ImageService {
    @Autowired
    ImageMapper imageMapper;
    @Autowired
    UserMapper userMapper;
    @Autowired
    ImageConMapper imageConMapper;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    Gson gson;
    private static final String IMAGE="IMAGE:";
    private static final String IMAGE_COUNT="IMAGE:COUNT:";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT=new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }
    @Override
    public Result alert(Integer id) {
        Integer userId= ThreadLocalUtil.getUser().getId();
        boolean res=imageMapper.alert(userId,id);
        if(res) return new Result<>().ok("已设置");
        return new Result<>().ok("头像未拥有");
    }

    @Override
    public Result buy(Integer id) {
        Image image = imageMapper.selectById(id);
        User user=userMapper.selectById(ThreadLocalUtil.getUser().getId());
        if(image==null||image.getStock()!=-1||user==null||user.getCoin()<image.getCost()) return new Result<>().error(400,"无法购买");

        //更新金币
        user.setCoin(user.getCoin()-image.getCost());
        userMapper.updateById(user);

        //插入拥有关系
        ImageCon imageCon=new ImageCon();
        imageCon.setImage_id(id);
        imageCon.setUserId(user.getId());
        imageConMapper.insert(imageCon);

        return new Result<>().ok("购买成功");
    }

    @Override
    public Result adminUpdate(Image image) {
        if(ThreadLocalUtil.getUser().getId()!=1) return new Result<>().error(400,"无权限");

        if(image.getId()!=null) imageMapper.updateById(image);
        else{
            imageMapper.insert(image);
        }
        stringRedisTemplate.opsForValue().set(IMAGE_COUNT+image.getId(), gson.toJson(image.getStock()));
        return new Result<>().ok("更新成功");
    }

    @Override
    public Result sale(Integer id) {
        User user= userMapper.selectById(ThreadLocalUtil.getUser().getId());

        ImageCon imageCon=null;

        //imageCon = imageConMapper.selectOne(Wrappers.<ImageCon>lambdaQuery().eq(ImageCon::getUserId, user.getId()).eq(ImageCon::getImage_id, id));
        //if(imageCon!=null) return new Result<>().error(400,"请勿重复购买");

        imageCon=new ImageCon();
        imageCon.setUserId(user.getId());
        imageCon.setImage_id(id);

        String key=IMAGE+id;//分布式锁名
        String count_key=IMAGE_COUNT+id;

        //抢购结束防止骚扰数据库
        Integer count = gson.fromJson(stringRedisTemplate.opsForValue().get(count_key), new TypeToken<Integer>() {}.getType());
        if(count==null||count<=0) return new Result<>().error(400,"限购已结束");

        int retry=10;//重试10次获取锁
        while(retry>0)
        {
            if(getLock(key)) {
                Image image = imageMapper.selectById(id);
                if (image == null || image.getStock() <= 0 || image.getCost() > user.getCoin()) {
                    //release lock
                    unLock(key);
                    return new Result<>().error(400, "购买失败");
                }
                user.setCoin(user.getCoin() - image.getCost());
                image.setStock(image.getStock() - 1);
                userMapper.updateById(user);
                imageMapper.updateById(image);
                imageConMapper.insert(imageCon);
                //数量放入redis缓存防止结束后依旧骚扰数据库
                stringRedisTemplate.opsForValue().set(count_key, gson.toJson(image.getStock()));
                //releaselock
                unLock(key);
                return new Result<>().ok("购买成功");
            }
            retry--;
        }
        unLock(key);
        return new Result<>().error(400,"购买失败");
    }

    @Override
    public Result show() {
        List<Image> list=imageMapper.show();
        return new Result<>().ok(list);
    }

    private Boolean getLock(String key)
    {
        long id = Thread.currentThread().getId();
        return stringRedisTemplate.opsForValue().setIfAbsent(key, gson.toJson(id),1, TimeUnit.SECONDS);
    }

    private void unLock(String key)
    {
        long id=Thread.currentThread().getId();
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key),gson.toJson(id));
    }

}
