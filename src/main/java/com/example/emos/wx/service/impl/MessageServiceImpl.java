package com.example.emos.wx.service.impl;

import com.example.emos.wx.db.mapper.MessageMapper;
import com.example.emos.wx.db.mapper.MessageRefMapper;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRefEntity;
import com.example.emos.wx.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private MessageRefMapper messageRefMapper;

    // 向Message集合插入数据
    @Override
    public String insertMessage(MessageEntity entity) {

        String id = messageMapper.insert(entity);
        return id;
    }

    // 向MessageRef集合插入数据
    @Override
    public String insertRef(MessageRefEntity entity) {
        String id = messageRefMapper.insert(entity);
        return id;
    }

    // 查询未读消息数量
    @Override
    public long searchUnreadCount(int userId) {
        long count = messageRefMapper.searchUnreadCount(userId);
        return count;
    }

    // 查询新接收的消息的数量
    @Override
    public long searchLastCount(int userId) {
        long count = messageRefMapper.searchLastCount(userId);
        return count;
    }

    // 查询分页数据
    @Override
    public List<HashMap> searchMessageByPage(int userId, long start, int length) {
        List<HashMap> list = messageMapper.searchMessageByPage(userId, start, length);
        return list;
    }

    // 根据ID查询消息
    @Override
    public HashMap searchMessageById(String id) {
        HashMap map = messageMapper.searchMessageById(id);
        return map;
    }

    // 把消息从未读改成已读
    @Override
    public long updateUnreadMessage(String id) {
        long rows = messageRefMapper.updateUnreadMessage(id);
        return rows;
    }

    // 根据消息ID删除消息
    @Override
    public long deleteMessageRefById(String id) {
        long rows = messageRefMapper.deleteMessageRefById(id);
        return rows;
    }

    // 根据userID删除消息
    @Override
    public long deleteUserMessageRef(int userId) {
        long rows=messageRefMapper.deleteUserMessageRef(userId);
        return rows;
    }
}
