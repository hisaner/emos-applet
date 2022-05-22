package com.example.emos.wx.service;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRefEntity;

import java.util.HashMap;
import java.util.List;

public interface MessageService {
    // 向Message集合插入数据
    public String insertMessage(MessageEntity entity);

    // 向MessageRef集合插入数据
    public String insertRef(MessageRefEntity entity);

    // 查询未读消息数量
    public long searchUnreadCount(int userId);

    // 查询新接收的消息的数量
    public long searchLastCount(int userId);

    // 查询分页数据
    public List<HashMap> searchMessageByPage(int userId, long start, int length);

    // 根据ID查询消息
    public HashMap searchMessageById(String id);

    // 把消息从未读改成已读
    public long updateUnreadMessage(String id);

    // 根据消息ID删除消息
    public long deleteMessageRefById(String id);

    // 根据userID删除消息
    public long deleteUserMessageRef(int userId);
}
