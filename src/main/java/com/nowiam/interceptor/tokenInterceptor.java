package com.nowiam.interceptor;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nowiam.model.Result;
import com.nowiam.model.pojo.User;
import com.nowiam.util.ThreadLocalUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class tokenInterceptor implements HandlerInterceptor {
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    Gson gson;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //获取token
        String token = request.getHeader("token");

        if(request.getRequestURI().contains("/login")||request.getRequestURI().contains("/register")){
            //放行
            if(token!=null)
            {
                stringRedisTemplate.delete("USER:"+token);
            }
            return true;
        }
        //String hello = stringRedisTemplate.opsForValue().get("hello");
        //System.out.println("----------------------redis-----------------------------:"+hello);

        //判断token是否存在
        if(StringUtils.isBlank(token)){
            returnNoLogin(response);
            return false;
        }

        String str = stringRedisTemplate.opsForValue().get("USER:"+token);
        if(str==null)
        {
            returnNoLogin(response);
            return false;
        }
        //stringRedisTemplate.opsForValue().set("USER:"+token,str,30, TimeUnit.MINUTES);
        stringRedisTemplate.expire("USER:"+token,30, TimeUnit.MINUTES);
        User user = gson.fromJson(str, new TypeToken<User>() {}.getType());
        ThreadLocalUtil.setUser(user);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        ThreadLocalUtil.clear();
    }

    private void returnNoLogin(HttpServletResponse response) throws IOException {
        ServletOutputStream outputStream = response.getOutputStream();
        // 设置返回401 和响应编码
        response.setStatus(401);
        response.setContentType("Application/json;charset=utf-8");
        // 构造返回响应体
        Result result=new Result<>().error(400,"未登陆，请先登陆");
        Gson gson=new Gson();
        String resultString = gson.toJson(result);
        outputStream.write(resultString.getBytes(StandardCharsets.UTF_8));
    }
}
