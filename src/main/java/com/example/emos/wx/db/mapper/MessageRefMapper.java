package com.example.emos.wx.db.mapper;

import com.example.emos.wx.db.pojo.MessageRefEntity;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class MessageRefMapper {
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     *  MongoDB中新增消息接收状态 保存记录并返回
     */
    public String insert(MessageRefEntity entity) {
        entity = mongoTemplate.save(entity);
        return entity.get_id();
    }

    /**
     * 查询用户未读消息数量
     * 汇总统计，返回类型必须是 long 类型
     */
    public long searchUnreadCount(int userId) {
        Query query = new Query();
        // 封装查询条件 (未读，userid)
        query.addCriteria(Criteria.where("readFlag").is(false).and("receiverId").is(userId));
        // count函数做统计 (Query对象,映射类的类型)
        long count = mongoTemplate.count(query, MessageRefEntity.class);
        return count;
    }

    /**
     * 查询新接收消息数量 实质是修改替代查询
     */
    public long searchLastCount(int userId) {
        Query query = new Query();
        // 封装查询条件 (最新消息，接收人id)
        query.addCriteria(Criteria.where("lastFlag").is(true).and("receiverId").is(userId));
        Update update = new Update();
        update.set("lastFlag", false);
        // updateMulti(query对象,update对象,映射类)
        UpdateResult result = mongoTemplate.updateMulti(query, update, "message_ref");
        long rows = result.getModifiedCount();
        return rows;
    }

    /**
     * 把未读消息变更为已读消息
     */
    public long updateUnreadMessage(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("readFlag", true);
        // updateMulti(query对象,update对象,映射类)
        UpdateResult result = mongoTemplate.updateFirst(query, update, "message_ref");
        long rows = result.getModifiedCount();
        return rows;
    }

    /**
     * 根据ID删除ref消息记录
     */
    public long deleteMessageRefById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        DeleteResult result = mongoTemplate.remove(query, "message_ref");
        long rows = result.getDeletedCount();
        return rows;
    }

    /**
     * 删除某个用户全部消息
     */
    public long deleteUserMessageRef(int userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("receiverId").is(userId));
        DeleteResult result = mongoTemplate.remove(query, "message_ref");
        long rows = result.getDeletedCount();
        return rows;
    }
}
