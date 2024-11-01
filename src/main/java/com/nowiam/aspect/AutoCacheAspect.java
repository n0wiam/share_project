package com.nowiam.aspect;

import com.google.gson.Gson;
import com.nowiam.model.pojo.Note;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Aspect
public class AutoCacheAspect {
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    Gson gson;
    @Pointcut("execution(* com.nowiam.mapper.*.*(..)) && @annotation(com.nowiam.annotation.AutoCache)")
    public void add(){};

    @Pointcut("execution(* com.nowiam.mapper.*.*(..)) && @annotation(com.nowiam.annotation.ClearCache)")
    public void clear(){};

    @AfterReturning(value = "add()",returning = "result")
    public void fun(JoinPoint joinPoint,Object result){
        Integer userId= (Integer) joinPoint.getArgs()[0];
        Integer status= (Integer) joinPoint.getArgs()[1];

        List<Note> res= (List<Note>) result;

        String key="List:"+userId+":"+status;

        String mes=gson.toJson(res);

        stringRedisTemplate.opsForValue().set(key,mes);
    }

//    @Before("clear()")
//    public void fun2(JoinPoint joinPoint){
//
//    }

}
