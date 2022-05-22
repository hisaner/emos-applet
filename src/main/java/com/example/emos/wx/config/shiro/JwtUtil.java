package com.example.emos.wx.config.shiro;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
//JwtUtil工具类
@Component
@Slf4j
public class JwtUtil {
    @Value("${emos.jwt.secret}")//注入secret值
    private String secret;
    @Value("${emos.jwt.expire}")
    private int expire;

    public String createToken(int userId) {
        Date date = DateUtil.offset(new Date(), DateField.DAY_OF_YEAR, 5);//计算过期日期，偏移5天
        Algorithm algorithm=Algorithm.HMAC256(secret);//密钥封装成加密算法对象
        JWTCreator.Builder builder= JWT.create();//创建内部类对象
        String token = builder.withClaim("userId", userId).withExpiresAt(date).sign(algorithm);//生成密钥
        return token;
    }

    public int getUserId(String token){
        DecodedJWT jwt = JWT.decode(token);//解码
        int userId = jwt.getClaim("userId").asInt();
        return userId;
    }

    public void verifierToken(String token) {
        Algorithm algorithm=Algorithm.HMAC256(secret);//创建算法对象
        JWTVerifier verifier = JWT.require(algorithm).build();//创建验证对象
        verifier.verify(token);//验证token。-无需捕获异常，一旦异常会抛出runtime异常
    }
}
