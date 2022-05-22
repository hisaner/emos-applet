package com.example.emos.wx.config;

import com.baidu.aip.face.AipFace;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 百度人脸识别主类
 * @author：userqiao
 * @email：userqiao@163.com
 * @date：2022/2/26 23:32
 */
@Configuration
public class BaiduSample {
    @Value("${emos.baidu.appId}")
    private String APP_ID;
    @Value("${emos.baidu.apiKey}")
    private String API_KEY;
    @Value("${emos.baidu.secretKey}")
    private String SECRET_KEY;

    @Bean
    public AipFace initApiFace(){
        // 初始化ApiFace
        AipFace client = new AipFace(APP_ID, API_KEY, SECRET_KEY);
        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);

        return client;
    }
}
