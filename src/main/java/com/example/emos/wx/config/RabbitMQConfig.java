package com.example.emos.wx.config;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//RabbitMQ配置类
@Configuration
public class RabbitMQConfig {
    @Bean
    public ConnectionFactory getFactory(){
        ConnectionFactory factory = new ConnectionFactory();//连接RabbitMQ 需要用到ConnectionFactory
        factory.setHost("112.74.112.97");
        factory.setPort(5672);////RabbitMQ端口号
        return factory;
    }
}
