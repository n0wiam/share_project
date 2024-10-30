package com.nowiam.interceptor;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.google.gson.Gson;
import com.nowiam.model.Result;
import com.nowiam.model.pojo.User;
import com.nowiam.util.ThreadLocalUtil;
import io.jsonwebtoken.Claims;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class tokenInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(request.getRequestURI().contains("/login")||request.getRequestURI().contains("/register")){
            //放行
            return true;
        }
//        String method = request.getMethod();
//        if("OPTIONS".equals(method)) return true;
        //3.获取token
        String token = request.getHeader("token");
        //4.判断token是否存在
        if(StringUtils.isBlank(token)){
            returnNoLogin(response);
            return false;
        }
//        Claims claimsBody =null;
//        try{
//            claimsBody = AppJwtUtil.getClaimsBody(token);
//            //是否是过期
//            int result = AppJwtUtil.verifyToken(claimsBody);
//            if(result == 1 || result  == 2){
//                returnNoLogin(response);
//                return false;
//            }
//        }catch (Exception e)
//        {
//            returnNoLogin(response);
//            return false;
//        }

        //获得token解析后中的用户信息
        //Integer userId = (Integer) claimsBody.get("id");
        Integer userId=Integer.parseInt(token);
        //if(userId!=null)
        {
            User user=new User();
            user.setId(userId);
            ThreadLocalUtil.setUser(user);
        }
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
