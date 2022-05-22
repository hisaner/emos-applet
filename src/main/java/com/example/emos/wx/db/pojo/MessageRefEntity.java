package com.example.emos.wx.db.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
//消息模块MongoDB映射message_ref集合。信息ref消费者相关
@Data
@Document(collection = "message_ref")
public class MessageRefEntity implements Serializable {

    @Id
    private String _id;

    /**
     * message记录的_id
     */
    @Indexed
    private String messageId;

    /**
     * 接收人ID
     */
    @Indexed
    private Integer receiverId;

    /**
     * 是否已读
     */
    @Indexed
    private Boolean readFlag;

    /**
     * 是否为新接收的消息
     */
    @Indexed
    private Boolean lastFlag;
}
