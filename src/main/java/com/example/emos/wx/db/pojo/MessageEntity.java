package com.example.emos.wx.db.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;
//消息模块MongoDB映射message集合。信息主体
@Data
@Document(collection = "message")
public class MessageEntity implements Serializable {

    @Id
    private String _id;

    /**
     * UUID值，并且设置有唯一性索引，防止消息被重复消费
     */
    @Indexed(unique = true)
    private String uuid;

    /**
     * 发送者ID，就是用户ID。如果是系统自动发出，这个ID值是0
     */
    @Indexed
    private Integer senderId;

    /**
     * 发送者的头像URL。在消息页面要显示发送人的头像
     * https://static-wx-emos-1307625276.cos.ap-shanghai.myqcloud.com/img/system-photo/system.jpeg
     */
    private String senderPhoto = "https://static-1258386385.cos.ap-beijing.myqcloud.com/img/System.jpg";

    /**
     * 发送者名称，也就是用户姓名。在消息页面要显示发送人的名字
     */
    private String senderName;

    /**
     * 消息正文
     */
    private String msg;

    /**
     * 发送时间
     */
    @Indexed
    private Date sendTime;

}