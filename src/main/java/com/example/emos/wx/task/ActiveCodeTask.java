package com.example.emos.wx.task;

import cn.hutool.core.util.RandomUtil;
import com.example.emos.wx.db.mapper.TbUserMapper;
import com.example.emos.wx.db.pojo.TbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
@Scope("prototype")
public class ActiveCodeTask {
    @Autowired
    private EmailTask emailTask;

    @Autowired
    private TbUserMapper userMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Async("AsyncTaskExecutor")
    public void sendActiveCodeAsync(int userId, String email) {
        String activeCode = null;
        while (true) {
            //生成激活码
            activeCode = RandomUtil.randomInt(100000, 999999) + "";
            if (redisTemplate.hasKey(activeCode)) {
                continue;
            } else {
                break;
            }
        }
        //激活码有效期为1天
        redisTemplate.opsForValue().set(activeCode, userId + "", 1, TimeUnit.DAYS);
        TbUser tbUser = userMapper.searchById(userId);

        System.out.println("activeCode--->"+activeCode);
        //发送邮件
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("在线办公系统新员工激活码");
        msg.setText(tbUser.getName()+"您好，你的新员工办公系统激活码是：" + activeCode+"。请当天尽快激活进入系统");
        emailTask.sendAsync(msg);
    }
}