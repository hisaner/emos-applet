package com.example.emos.wx.task;

import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRefEntity;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.MessageService;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/*
 * @author saner
 * @date 2022/04/24 14:18
 * @description 消息发送同步执行异步执行
 */
@Slf4j
@Component
public class MessageTask {

    @Autowired
    private ConnectionFactory factory;

    @Autowired
    private MessageService messageService;

    /**
     * 同步发送消息
     * @param topic  主题
     * @param entity 消息发送对象
     */
    public void send(String topic, MessageEntity entity) {
        String id = messageService.insertMessage(entity);       //保存数据到MongoDB并且获取消息主键id
        //向RabbitMQ发送消息
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(topic, true, false, false, null);  //连接到某个topic
            HashMap header = new HashMap();     //存放属性数据
            header.put("messageId", id);
            //创建AMQP协议参数对象，添加附加属性
            final AMQP.BasicProperties basicProperties = new AMQP.BasicProperties().builder().headers(header).build();
            channel.basicPublish("", topic, basicProperties, entity.getMsg().getBytes());
            log.debug("消息发送成功！");
        } catch (Exception e) {
            log.error("消息发送失败：{}" + e.getMessage());
            throw new EmosException("消息发送失败");
        }
    }

    /**
     * 异步发送消息
     * @param topic  主题
     * @param entity 消息发送对象
     */
    @Async
    public void sendAsync(String topic, MessageEntity entity) {
        send(topic, entity);
    }

    /**
     * 同步接收消息
     * @param topic 主题
     * @return 接收消息数量
     */
    public int receive(String topic) {
        int i = 0;
        //接收消息数据
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel())
        {
            channel.queueDeclare(topic, true, false, false, null);     //从队列中获取消息, 不自动确认
            //topic主题中不确定有多少条数据，使用死循环遍历直到没有后退出循环
            while (true) {
                GetResponse response = channel.basicGet(topic,false);
                if (response != null) {
                    AMQP.BasicProperties properties = response.getProps();
                    Map<String, Object> headers = properties.getHeaders();             //获取附加属性对象
                    String messageId = headers.get("messageId").toString();
                    byte[] body = response.getBody();                                  //获取消息正文
                    String message = new String(body);
                    log.debug("从RabbitMQ中接收到的消息: " + message);
                    MessageRefEntity messageRefEntity = new MessageRefEntity();
                    messageRefEntity.setMessageId(messageId);
                    messageRefEntity.setReceiverId(Integer.parseInt(topic));
                    messageRefEntity.setReadFlag(false);
                    messageRefEntity.setLastFlag(true);
                    messageService.insertRef(messageRefEntity);                 //接收消息数据存储在MongoDB中
                    //将数据保存在MongoDB之后，在响应ack应答，让topic删除中数据
                    long deliveryTag = response.getEnvelope().getDeliveryTag();
                    channel.basicAck(deliveryTag, false);
                    i++;
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("接收消息异常：{}" + e.getMessage());
        }
        return i;
    }

    /**
     * 异步接收消息
     * @param topic 主题
     * @return 接收消息数量
     */
    @Async
    public int receiveAsync(String topic) {
        return receive(topic);
    }

    /**
     * 同步删除消息队列
     * @param topic 主题
     */
    public void deleteQueue(String topic) {
        try(Connection connection = factory.newConnection();
            Channel channel = connection.createChannel())
        {
            channel.queueDelete(topic);
            log.debug("成功删除消息队列");
        } catch (Exception e) {
            log.error("删除消息队列失败：{}" + e.getMessage());
            throw new EmosException("删除消息队列失败");
        }
    }

    /**
     * 异步删除消息队列
     * @param topic 主题
     */
    @Async
    public void deleteQueueAsync(String topic) {
        deleteQueue(topic);
    }

}

