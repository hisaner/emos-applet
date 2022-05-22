package com.example.emos.wx.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.emos.wx.db.mapper.TbMeetingMapper;
import com.example.emos.wx.db.mapper.TbUserMapper;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.TbMeeting;
import com.example.emos.wx.db.pojo.TbUser;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.MeetingService;
import com.example.emos.wx.task.MessageTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.*;

/*
 * @author saner
 * @date 2022/4/25 15:32
 * @description 会议列实现类
 */
/**
 *  会议status 1是刚创建 2是审核未通过 3是审核通过但未开始 4是正在进行中 5是结束了 6是已删除
 */
@Service
@Slf4j
public class MeetingServiceImpl implements MeetingService {
    @Autowired
    private TbMeetingMapper meetingMapper;
    //工作流注入
    @Autowired
    private TbUserMapper userMapper;
    @Value("${emos.code}")
    private String code;
    @Value("${workflow.url}")
    private String workflow;

    @Value("${emos.recieveNotify}")
    private String recieveNotify;
    @Autowired
    private MessageTask messageTask;
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 添加会议
     * @param
     */
/*    @Override
    public void insertMeeting(TbMeeting entity) {
        //保存数据
        int row = meetingMapper.insertMeeting(entity);
        if (row != 1) {
            throw new EmosException("会议添加失败");
        }
        //开启审批工作流
        String meetingType = entity.getType() == 1 ? "线上会议" : "线下会议";
        startMeetingWorkflow(entity.getUuid(),entity.getCreatorId().intValue(),entity.getDate(), entity.getStart());
    }*/
    @Override
    public void insertMeeting(TbMeeting meeting) {
        // 这里把参会人员 放入一个数组里 然后放在member字段
        int row = meetingMapper.insertMeeting(meeting);
        if (row != 1) {
            throw new EmosException("会议添加失败");
        }
    }
    /**
     * 查询会议列表 分页数据
     * 按照日期进行分组
     * @param
     * @return
     */
/*    @Override
    public ArrayList<HashMap> searchMyMeetingListByPage(HashMap param) {
        ArrayList<HashMap> list = meetingMapper.searchMyMeetingListByPage(param); // 得到查询结果
        ArrayList resultList = new ArrayList(); // 创建新list保存会议列表
        String date = null;
        HashMap resultMap = null;
        JSONArray array = null;
        for (HashMap map : list) {
            String temp = map.get("date").toString(); // 获取会议日期
            if (!temp.equals(date)) {
                date = temp;
                resultMap = new HashMap();
                array = new JSONArray();
                resultList.add(resultMap);//新的会议小列表
                resultMap.put("date", date);
                resultMap.put("list", array);
            }
            array.put(map);
        }
        return resultList;
    }*/
    @Override
    public ArrayList<HashMap> searchMyMeetingListByPage(HashMap params) {
        // 先根据我的id查出所有会议
        ArrayList<HashMap> list = meetingMapper.searchMyMeetingListByPage(params); //拿到了会议列表数据
        String date = null;
        ArrayList resultList = new ArrayList();
        HashMap resultMap = null;
        JSONArray array = null;

        // 这一块代码全是删除会议的和改变会议类型的
        Iterator<HashMap> it = list.iterator();
        while (it.hasNext()) {
            HashMap map = it.next();
            DateTime startTime = DateUtil.parse((String) map.get("date") + " " + (String) map.get("start") + ":00");

            DateTime endTime = DateUtil.parse((String) map.get("date") + " " + (String) map.get("end") + ":00");
            DateTime now = DateUtil.date();
            DateTime delete_time = DateUtil.offsetDay(endTime, Integer.parseInt("7")); // 负数是往前提

            // 会议时间的3天之后，就逻辑删掉这个会议
            if (now.isAfterOrEquals(delete_time)) {
                int id = Integer.parseInt(map.get("id").toString());
                _updateMeetingToDelete(id); // 改成逻辑删除吧
                it.remove();    // 不要它了 这也就是为什么用遍历器的原因
            }
            // 如果 现在的时间在会议时间之后
            else if (now.isAfter(endTime)) {
                int id = Integer.parseInt(map.get("id").toString()); // 会议id
                _updateMeetingOver(id);
                map.put("status", 5);   // 代表过期了
//                it.remove();
            }
            // 如果 正在开会时
            else if (now.isAfter(startTime)&&now.isBefore(endTime)){
                System.out.println("startTime-->"+startTime);
                System.out.println("endTime"+endTime);
                System.out.println("now--->"+now);

                int id = Integer.parseInt(map.get("id").toString()); // 会议id
                _updateMeetingToStarting(id);
                map.put("status", 4);
            }
        }

        //
        for (HashMap map : list) {
            //会议日期是肯定有的
            String temp = map.get("date").toString();
            // 之前写的date.equals(temp) 导致了空指针异常 因为date初始为空
            // 拿到的日期如果等于旧日期 把旧日期的小列表加上 map 就行
            // map里面放的是 很多会议的内容
            // 如果拿到的日期不等于旧日期 就说明是新日期
            // 新日期就要new一个新的jsonArray 然后把这个

            // 当旧日期不等于新日期时 把相同日期的会议放入一个临时map里
            if (!temp.equals(date)) {
                date = temp;
                resultMap = new HashMap();
                resultMap.put("date", date);
                array = new JSONArray();
                //  仔细看这段代码 即使先添加到map里 在改value的值 map的数字也会变
                resultMap.put("list", array);
                resultList.add(resultMap);
            }
            // 测试一个小功能 看看查看的这个人和创建人是不是同一个人
            Integer creatorId = Integer.parseInt((String) map.get("u2Id")) ; // 创建人id
            Integer userId = (Integer) params.get("userId");   // 查询人id
            if (Integer.compare(creatorId,userId)==0){
                // 相同 可编辑
                map.put("isSamePeople",true);
            }else {
                map.put("isSamePeople",false);
            }
            //会议小列表 因为每天可能有不同会议 所以需要有个列表存储同一天内的不同会议
            array.put(map);
        }
        return resultList;
    }
    @Override
    // 部门经理才能查询啊 有权限就行
    public ArrayList<HashMap> searchMeetingByManagerDept(HashMap param) {
        ArrayList<HashMap> list = meetingMapper.searchMeetingByManagerDept(param);
        String date = null;
        ArrayList resultList = new ArrayList();
        HashMap resultMap =null;
        JSONArray array = null;
        for(HashMap map:list){
            String temp = (String) map.get("uuid");
            if (map.get("approvalId") != null) {
                int lastUserId = (int) map.get("approvalId");
                map.put("lastUser", lastUserId);
            }
            int result_1 = (int) map.get("status");
            System.out.println("temp"+temp);
            boolean sameDept = meetingMapper.searchMeetingMembersInSameDept(temp);
            map.put("sameDept", sameDept);
            map.put("result_1", result_1);

        }
/*        for(HashMap map:list){

            String temp = map.get("date").toString();
            // 之前写的date.equals(temp) 导致了空指针异常
            if (!temp.equals(date)){    // 当旧日期不等于新日期时 把
                date=temp;
                resultMap=new HashMap();
                resultMap.put("date",date);
                array = new JSONArray();
                resultMap.put("list",array);
                resultList.add(resultMap);
            }
            //会议小列表 因为每天可能有不同会议 所以需要有个列表存储同一天内的不同会议
            array.put(map);
            // resultList > resultMap > array
        }
        return resultList;*/
        return list;
    }
    @Override
    public int approvalMeetingInfo(int flag, int id,int approvalId) {
        if (flag == 0) {
            ArrayList<HashMap> maps = meetingMapper.searchMeetingMembers(id);
            for(HashMap map:maps){
                String userId =  map.get("id").toString();
                String msg = "你有新的会议，请及时查看。";
                MessageEntity entity = new MessageEntity();
                entity.setSenderId(0);  //系统自动发出
                entity.setSenderPhoto("../../static/icon-meeting.png");
                entity.setSenderName("会议消息");
                entity.setMsg(msg);
                entity.setSendTime(new Date());
                messageTask.sendAsync(userId.toString(), entity);
            }
            return meetingMapper.updateMeetingSuccessById(id,approvalId);

        } else if (flag == 1) {
            return meetingMapper.updateMeetingFailById(id,approvalId);
        }

        return -1;
    }


    //通过id查询会议，把成员数组放入会议map中
    @Override
    public HashMap searchMeetingById(int id) {
        HashMap map = meetingMapper.searchMeetingById(id);
        ArrayList<HashMap> list = meetingMapper.searchMeetingMembers(id);
        map.put("members", list);
        return map;
    }

/*    @Override
    public void updateMeetingInfo(HashMap param) {
        int id = (int) param.get("id");
        String date = param.get("date").toString();
        String start = param.get("start").toString();
        String instanceId = param.get("instanceId").toString();
        HashMap oldMeeting = meetingMapper.searchMeetingById(id);
        String uuid = oldMeeting.get("uuid").toString();
        Integer creatorId = Integer.parseInt(oldMeeting.get("creatorId").toString());
        int row = meetingMapper.updateMeetingInfo(param);
        if (row != 1) {
            throw new EmosException("会议更新失败");
        }
        JSONObject json = new JSONObject();
        json.set("instanceId", instanceId);
        json.set("reason", "会议被修改");
        json.set("uuid", uuid);
        json.set("code", code);
        String url = workflow + "/workflow/deleteProcessById";
        HttpResponse resp = HttpRequest.post(url).header("content-type", "application/json")
                .body(json.toString()).execute();
        if (resp.getStatus() != 200) {
            log.error("删除工作流失败");
            throw new EmosException("删除工作流失败");
        }
        startMeetingWorkflow(uuid, creatorId, date, start);
    }*/
@Override
public void updateMeetingInfo(HashMap param) {
    int id = (int) param.get("id");
    String date = param.get("date").toString();
    String start = param.get("start").toString();
    String instanceId = param.get("instanceId").toString();
    HashMap oldMeeting = meetingMapper.searchMeetingById(id);
    String uuid = oldMeeting.get("uuid").toString();
    Integer creatorId = Integer.parseInt(oldMeeting.get("creatorId").toString());
    int row = meetingMapper.updateMeetingInfo(param);
    if (row != 1) {
        throw new EmosException("会议更新失败");
    }
//    return row;
}


/*    @Override
    public void deleteMeetingById(int id) {
        HashMap meeting = meetingMapper.searchMeetingById(id);
        String uuid = meeting.get("uuid").toString();
        String instanceId = meeting.get("instanceId").toString();
        DateTime date = DateUtil.parse(meeting.get("date") + " " + meeting.get("start"));
        DateTime now = DateUtil.date();
        if (now.isAfterOrEquals(date.offset(DateField.MINUTE, -20))) {
            throw new EmosException("距离会议开始不足20分钟，不能删除会议");
        }
        int row = meetingMapper.deleteMeetingById(id);
        if (row != 1) {
            throw new EmosException("会议删除失败");
        }
        JSONObject json = new JSONObject();
        json.set("instanceId", instanceId);
        json.set("reason", "会议被修改");
        json.set("uuid", uuid);
        json.set("code", code);
        String url = workflow + "/workflow/deleteProcessById";
        HttpResponse resp = HttpRequest.post(url).header("content-type", "application/json")
                .body(json.toString()).execute();
        if (resp.getStatus() != 200) {
            log.error("删除工作流失败");
            throw new EmosException("删除工作流失败");
        }
    }*/
@Override
public void deleteMeetingById(int id) {
    HashMap meeting = meetingMapper.searchMeetingById(id); // 查询会议信息
    DateTime time = DateUtil.parse(meeting.get("date") + " " + meeting.get("start"));
    int status = (int) meeting.get("status");
    DateTime now = new DateTime();
    if (status ==4) {
        throw new EmosException("会议正在进行中,不能删除");
    }
    if (now.isAfterOrEquals(time.offset(DateField.MINUTE, -20))&&now.isBeforeOrEquals(time)) {
        throw new EmosException("距离会议不足20分钟 不能删除会议");
    }
    int row = _deleteMeetingOver(id);
    if (row != 1)
        throw new EmosException("删除会议失败");
//    return row;
}
    @Override
    public Long searchRoomIdByUUID(String uuid) {
        Object temp=redisTemplate.opsForValue().get(uuid);//redisTemplate中无uuid
        long roomId=Long.parseLong(temp.toString());
        return roomId;
    }


    @Override
    public List<String> searchUserMeetingInMonth(HashMap param) {
        List<String> list = meetingMapper.searchUserMeetingInMonth(param);
        return list;
    }


    //以下开始工作流内容
    private void startMeetingWorkflow(String uuid, int creatorId, String date, String start) {
        HashMap info = userMapper.searchUserInfo(creatorId); //查询创建者用户信息

        JSONObject json = new JSONObject();
        json.set("url", recieveNotify);
        json.set("uuid", uuid);
        json.set("openId", info.get("openId"));
        json.set("code",code);
        json.set("date",date);
        json.set("start",start);
        String[] roles = info.get("roles").toString().split("，");
        //如果不是总经理创建的会议
        if (!ArrayUtil.contains(roles, "总经理")) {
            //查询总经理ID和同部门的经理的ID
            Integer managerId = userMapper.searchDeptManagerId(creatorId);
            json.set("managerId", managerId); //部门经理ID
            Integer gmId = userMapper.searchGmId();//总经理ID
            json.set("gmId", gmId);
            //查询会议员工是不是同一个部门
            boolean bool = meetingMapper.searchMeetingMembersInSameDept(uuid);
            json.set("sameDept", bool);
        }
        String url = workflow+"/workflow/startMeetingProcess";
        //请求工作流接口，开启工作流
        System.out.println("json"+json);

        HttpResponse response = HttpRequest.post(url).header("Content-Type", "application/json").body(json.toString()).execute();

        System.out.println("response:"+response);

        if (response.getStatus() == 200) {
            json = JSONUtil.parseObj(response.body());
            //如果工作流创建成功，就更新会议状态
            String instanceId = json.getStr("instanceId");
            HashMap param = new HashMap();
            param.put("uuid", uuid);
            param.put("instanceId", instanceId);
            int row = meetingMapper.updateMeetingInstanceId(param); //在会议记录中保存工作流实例的ID
            if (row != 1) {
                throw new EmosException("保存会议工作流实例ID失败");
            }
        }
    }

    // 会议结束了
    private int _updateMeetingOver(int id) {
        int row = meetingMapper.updateMeetingOver(id);
        if (row != 1)
            throw new EmosException("更新会议失败");
        return row;
    }
    private int _deleteMeetingOver(int id) {
        int row = meetingMapper.deleteMeetingInfo(id);
        if (row != 1)
            throw new EmosException("删除会议失败");
        return row;
    }
    private int _updateMeetingToDelete(int id) {
        int row = meetingMapper.updateMeetingToDelete(id);
        if (row != 1)
            throw new EmosException("逻辑删除会议失败");
        return row;
    }

    private int _updateMeetingToStarting(int id) {
        int row = meetingMapper.updateMeetingToStarting(id);
        if (row != 1)
            throw new EmosException("更新到会议进行中失败");
        return row;
    }
}
