package com.example.emos.wx.aop;

import com.example.emos.wx.common.util.R;
import com.example.emos.wx.config.shiro.ThreadLocalToken;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
//AOP切面类把更新的令牌返回给R
@Aspect
@Component
public class TokenAspect {
    @Autowired
    private ThreadLocalToken threadLocalToken;

    @Pointcut("execution(public * com.example.emos.wx.controller.*.*(..))")
    public void aspect(){

    }
    @Around("aspect()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        R r=(R) point.proceed(); //得到web方法返回值
        String token=threadLocalToken.getToken();//若存在Token，说明是更新的Token
        if (token != null) {
            r.put("token", token);//将token添加到r对象，给客户端更新令牌用
            threadLocalToken.clear();//用完清空，防止内存泄漏
        }
        return r;
    }
}
