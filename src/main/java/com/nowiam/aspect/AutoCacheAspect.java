package com.nowiam.aspect;

import com.google.gson.Gson;
import com.nowiam.annotation.AutoCache;
import com.nowiam.util.ThreadLocalUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
@Aspect
public class AutoCacheAspect {
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    Gson gson;
    @Around(value = "execution(* com.nowiam.*.*.*(..)) && @annotation(autoCache)")
    public Object fun(ProceedingJoinPoint pjp, AutoCache autoCache) throws Throwable {
        String type= autoCache.value();
        int isMore= autoCache.ops();
        Integer userId= ThreadLocalUtil.getUser().getId();
        String key=type+":"+userId;

        if(isMore!=-1)
        {
            try {
                Integer status= (Integer) (pjp.getArgs()[isMore]);
                key+=":"+status;
            }catch (Exception e)
            {
                throw new Throwable("注解错误");
            }
        }
        Object result=null;
        try {
            result = pjp.proceed();
            System.out.println("尝试自动缓存:"+key);
        } catch (Throwable e) {
            System.out.println("异常通知:"+e.getMessage());
        }
        stringRedisTemplate.opsForValue().set(key,gson.toJson(result),5, TimeUnit.SECONDS);
        return result;
    }

}
