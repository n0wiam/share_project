package com.nowiam.aspect;

import com.google.gson.Gson;
import com.nowiam.annotation.AutoCaches;
import com.nowiam.util.ThreadLocalUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;


@Component
@Aspect
public class AutoCacheAspect {
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    Gson gson;
    @Around(value = "execution(* com.nowiam.*.*.*(..)) && @annotation(autoCaches)")
    public Object fun(ProceedingJoinPoint pjp,AutoCaches autoCaches) throws Throwable {
        String type=autoCaches.value();
        int isMore=autoCaches.ops();
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
            System.out.println("异常通知");
        }
        stringRedisTemplate.opsForValue().set(key,gson.toJson(result));
        return result;
    }

}
