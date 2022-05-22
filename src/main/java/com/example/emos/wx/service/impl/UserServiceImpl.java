package com.example.emos.wx.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.emos.wx.db.mapper.*;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.TbUser;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.UserService;
import com.example.emos.wx.task.ActiveCodeTask;
import com.example.emos.wx.task.MessageTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@Scope("prototype")
public class UserServiceImpl implements UserService {
    @Value("${wx.app-id}")
    private String appId;
    @Value("${wx.app-secret}")
    private String appSecret;

    @Autowired
    private TbUserMapper userMapper;
    @Autowired
    private TbDeptMapper deptMapper;
/*员工管理对持久层的操作*/
    @Autowired
    private TbCheckinMapper checkinMapper;
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private MessageRefMapper messageRefMapper;
    @Autowired
    private TbFaceModelMapper faceModelMapper;
    @Autowired
    private ActiveCodeTask activeCodeTask;
    @Autowired
    private MessageTask messageTask;
    @Autowired
    private RedisTemplate redisTemplate;
    // 将响应字符串转换为JSON对象，提起openId属性
    private String getOpenId(String code) {     //获取openid
        String url="https://api.weixin.qq.com/sns/jscode2session";
        HashMap map=new HashMap();
        map.put("appid",appId);
        map.put("secret", appSecret);
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String response = HttpUtil.post(url, map);
        JSONObject json = JSONUtil.parseObj(response);
        String openId = json.getStr("openid");
        if (openId == null || openId.length() == 0) {
            throw new RuntimeException("临时登陆凭证错误");
        }
        return openId;
    }

    @Override
    public int registerUser(String registerCode, String code, String nickname, String photo) {
        //如果邀请码是000000，代表是超级管理员
        if (registerCode.equals("000000")) {
            //查询超级管理员帐户是否已经绑定
            boolean bool = userMapper.haveRootUser();
            if (!bool) {
                //把当前用户绑定到ROOT帐户
                String openId = getOpenId(code);
                HashMap param = new HashMap();
                param.put("openId", openId);
                param.put("nickname", nickname);
                param.put("photo", photo);
                param.put("role", "[0]");
                param.put("status", 1);
                param.put("createTime", new Date());
                param.put("root", true);
                userMapper.insert(param);
                int id = userMapper.searchIdByOpenId(openId);

                // 当成功注册的时候，我们就利用MessageTask异步发送消息到MQ队列中
                MessageEntity entity = new MessageEntity();
                entity.setSenderId(0);
                entity.setSenderName("系统消息");
                entity.setUuid(IdUtil.simpleUUID());
                entity.setMsg("欢迎您注册成为超级管理员，请及时更新你的员工个人信息。");
                entity.setSendTime(new Date());
                messageTask.sendAsync(id + "", entity);

                return id;
            } else {
                //如果root已经绑定了，就抛出异常
                throw new EmosException("无法绑定超级管理员");
            }
        }else if (!redisTemplate.hasKey(registerCode)) {
            //判断邀请码是否有效
            throw new EmosException("激活码无效,请联系管理员");
        } else {
            int userId = Integer.parseInt(redisTemplate.opsForValue().get(registerCode).toString());
            TbUser tbUser = userMapper.searchById(userId);
            if (tbUser == null){
                throw new EmosException("找不到账号，请联系管理员");
            }
            //把当前用户绑定到ROOT帐户
            TbUser entity = new TbUser();
            String openId = getOpenId(code);
            HashMap param = new HashMap();
            param.put("openId", openId);
            param.put("nickname", nickname);
            param.put("photo", photo);
            param.put("userId", userId);
            int row = userMapper.activeUserAccount(param);
            if (row != 1) {
                throw new EmosException("账号激活失败");
            }
            // 员工注册成功后清楚Redis的缓存数据
            redisTemplate.delete(registerCode);
            // 当成功注册的时候，我们就利用MessageTask异步发送消息到MQ队列中
            MessageEntity message = new MessageEntity();
            message.setSenderId(0);
            message.setSenderName("系统消息");
            message.setUuid(IdUtil.simpleUUID());
            message.setMsg("欢迎您激活成功，请及时更新你的员工个人信息。");
            message.setSendTime(new Date());
            messageTask.sendAsync(userId + "", message);

            return userId;
        }
    }

    @Override
    public Set<String> searchUserPermissions(int userId) {
        Set<String> permissions =userMapper.searchUserPermissions(userId);
        return permissions;
    }
    //实现用户登陆功能
    @Override
    public Integer login(String code) {
        //使用临时授权字符串换取openId
        String openId = getOpenId(code);
        Integer id = userMapper.searchIdByOpenId(openId);
        if (id == null) {
            throw new EmosException("账户不存在");
        }
        // 从消息队列中接收消息，转移到消息表
//        messageTask.receiveAsync(id + "");
        return id;
    }

    @Override
    public TbUser searchById(int userId) {
        TbUser user = userMapper.searchById(userId);
        return user;
    }

    //查询员工的入职日期
    @Override
    public String searchUserHiredate(int userId) {
        String hiredate = userMapper.searchUserHiredate(userId);
        return hiredate;
    }

    //用户界面 查询用户概要信息
    @Override
    public HashMap searchUserSummary(int userId) {
        HashMap map = userMapper.searchUserSummary(userId);
        return map;
    }

    @Override
    public ArrayList<HashMap> searchUserGroupByDept(String keyword) {
        // 部门成员？ 根据 员工姓名 查询 部门信息
        ArrayList<HashMap> list1 = deptMapper.searchDeptMembers(keyword);
        // 根据部门查询 查询员工信息
        ArrayList<HashMap> list2 = userMapper.searchUserGroupByDept(keyword);
        for (HashMap one : list1) {
            long deptId = (long) one.get("id");
            ArrayList members = new ArrayList();
            for (HashMap two : list2) {
                long id = (long) two.get("deptId");  // 员工信息中的部门id
                if (id == deptId) {
                    members.add(two);
                }
            }
            one.put("members", members);
        }
        return list1;
    }

    @Override
    public ArrayList<HashMap> searchMembers(List param) {
        ArrayList<HashMap> list = userMapper.searchMembers(param);
        return list;
    }

    @Override
    public List<HashMap> selectUserPhotoAndName(List param) {
        List<HashMap> list = userMapper.selectUserPhotoAndName(param);
        return list;
    }

    @Override
    public String searchMemberEmail(int id) {
        String email = userMapper.searchMemberEmail(id);
        return email;
    }

    @Override
    public void insertUser(HashMap param) {
        //保存记录
        int row = userMapper.insert(param);
        if (row == 1) {
            String email = (String) param.get("email");
            //根据Email查找新添加用户的主键值
            int userId = userMapper.searchUserIdByEmail(email);
            //生成激活码，并且用邮件发送
            activeCodeTask.sendActiveCodeAsync(userId, email);
        } else {
            throw new EmosException("员工数据添加失败");
        }
    }

    @Override
    public void deleteUserById(int id) {
        int row = userMapper.deleteUserById(id); //删除员工数据
        if (row != 1) {
            throw new EmosException("删除员工失败");
        }
        checkinMapper.deleteUserCheckin(id);
        messageMapper.deleteUserMessage(id);
        messageRefMapper.deleteUserMessageRef(id);
        faceModelMapper.deleteFaceModel(id);
        messageTask.deleteQueue(id + "");
    }

    @Override
    public HashMap searchUserInfo(int userId) {
        HashMap map = userMapper.searchUserInfo(userId);
        return map;
    }

    @Override
    public int updateUserInfo(HashMap param) {
        //更新员工记录
        int rows = userMapper.updateUserInfo(param);
        //更新成功就发送消息通知
        if (rows == 1) {
            Integer userId = (Integer) param.get("userId");
            String msg = "你的个人资料已经被成功修改";
            MessageEntity entity = new MessageEntity();
            entity.setSenderId(0);  //系统自动发出
            entity.setSenderPhoto("../../static/system.jpg");
            entity.setSenderName("系统消息");
            entity.setMsg(msg);
            entity.setSendTime(new Date());
            messageTask.sendAsync(userId.toString(), entity);
        }
        return rows;
    }

    @Override
    public JSONObject searchUserContactList() {
        ArrayList<HashMap> list = userMapper.searchUserContactList();
        String letter = null;
        JSONObject json = new JSONObject(true);
        JSONArray array = null;
        for (HashMap<String, String> map : list) {
            String name = map.get("name");
            String firstLetter = PinyinUtil.getPinyin(name).charAt(0) + "";//取名字首字母
            firstLetter = firstLetter.toUpperCase();
            if (letter == null || !letter.equals(firstLetter)) {
                letter = firstLetter;
                array = new JSONArray();
                json.set(letter, array);
            }
            array.put(map);
        }
        return json;
    }
}
