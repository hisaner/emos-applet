package com.example.emos.wx.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import com.example.emos.wx.common.util.R;
import com.example.emos.wx.config.shiro.JwtUtil;
import com.example.emos.wx.controller.form.*;
import com.example.emos.wx.db.pojo.TbMeeting;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.MeetingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
 * @author saner
 * @date 2022/4/25 15:45
 * @description 会议模块网络接口
 */
@Slf4j
@RestController
@RequestMapping("/meeting")
@Api("会议模块网络接口")
public class MeetingController {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private MeetingService meetingService;



    //查询会议列表分页
    @PostMapping("/searchMyMeetingListByPage")
    @ApiOperation("查询会议列表分页数据 参会即可查询")
    public R searchMyMeetingListByPage(@Valid @RequestBody SearchMyMeetingListByPageForm form, @RequestHeader("token") String token) {
        int userId = jwtUtil.getUserId(token);
        int page = form.getPage();
        int length = form.getLength();
        long start = (page - 1) * length;
        HashMap map = new HashMap();
        map.put("userId", userId);
        map.put("start", start);
        map.put("length", length);
        ArrayList list = meetingService.searchMyMeetingListByPage(map);
        return R.ok().put("result", list);
    }

    @PostMapping("/insertMeeting")
    @ApiOperation("添加会议")
    @RequiresPermissions(value = {"ROOT", "MEETING:INSERT"}, logical = Logical.OR)
    public R insertMeeting(@Valid @RequestBody InsertMeetingForm form, @RequestHeader("token") String token) {
        // 判断时间是不是30天内创建的 如果
        DateTime meetingDate = DateUtil.parse(form.getDate());
        // 用tomorrow是因为 today 没有时间 只有日期
        DateTime futureTime = DateUtil.offsetDay(DateUtil.tomorrow(), 30);
        if (meetingDate.isAfter(futureTime)){
            throw new EmosException("不能创建一个月之后的会议");
        }
        if (form.getType() == 2 && (form.getPlace() == null || form.getPlace().length() == 0)) {
            throw new EmosException("线下会议地点不能为空");
        }
        DateTime d1 = DateUtil.parse(form.getDate() + " " + form.getStart() + ":00");
        DateTime d2 = DateUtil.parse(form.getDate() + " " + form.getEnd() + ":00");
        if (d2.isBeforeOrEquals(d1)) {
            throw new EmosException("结束时间必须大于开始时间");
        }
        if (!JSONUtil.isJsonArray(form.getMembers())) {
            throw new EmosException("members不是JSON数组");
        }
        TbMeeting entity = new TbMeeting();
        entity.setUuid(UUID.randomUUID().toString(true));// 会议id
        entity.setTitle(form.getTitle());
        entity.setCreatorId((long) jwtUtil.getUserId(token));// 创建者
        entity.setDate(form.getDate());
        entity.setPlace(form.getPlace());
        entity.setStart(form.getStart() + ":00");
        entity.setEnd(form.getEnd() + ":00");
        entity.setType((short) form.getType());
        entity.setMembers(form.getMembers());//参会人员
        entity.setDesc(form.getDesc());
        entity.setStatus((short) 1);
        entity.setInstanceId(UUID.randomUUID().toString(true));
        meetingService.insertMeeting(entity);
        return R.ok().put("result", "success");
    }

    @PostMapping("/searchMeetingById")
    @ApiOperation("根据ID查询会议")
    @RequiresPermissions(value = {"ROOT", "MEETING:SELECT"}, logical = Logical.OR)
    public R searchMeetingById(@Valid @RequestBody SearchMeetingByIdForm form){
        HashMap map=meetingService.searchMeetingById(form.getId());
        return R.ok().put("result",map);
    }


    @PostMapping("/updateMeetingInfo")
    @ApiOperation("更新会议")
    @RequiresPermissions(value = {"ROOT", "MEETING:UPDATE"}, logical = Logical.OR)
    public R updateMeetingInfo(@Valid @RequestBody UpdateMeetingInfoForm form){
        if(form.getType()==2&&(form.getPlace()==null||form.getPlace().length()==0)){
            throw new EmosException("线下会议地点不能为空");
        }
        DateTime d1= DateUtil.parse(form.getDate()+" "+form.getStart()+":00");
        DateTime d2= DateUtil.parse(form.getDate()+" "+form.getEnd()+":00");
        if(d2.isBeforeOrEquals(d1)){
            throw new EmosException("结束时间必须大于开始时间");
        }
        if(!JSONUtil.isJsonArray(form.getMembers())){
            throw new EmosException("members不是JSON数组");
        }
        HashMap param=new HashMap();
        param.put("title", form.getTitle());
        param.put("date", form.getDate());
        param.put("place", form.getPlace());
        param.put("start", form.getStart() + ":00");
        param.put("end", form.getEnd() + ":00");
        param.put("type", form.getType());
        param.put("members", form.getMembers());
        param.put("desc", form.getDesc());
        param.put("id", form.getId());
        param.put("instanceId", form.getInstanceId());
//        param.put("status", 1);
        param.put("status",3);
        meetingService.updateMeetingInfo(param);
        return R.ok().put("result","success");
    }

    @PostMapping("/deleteMeetingById")
    @ApiOperation("根据ID删除会议  逻辑删除")
    @RequiresPermissions(value = {"ROOT","MEETING:DELETE"},logical = Logical.OR)
    public R deleteMeetingById(@Valid @RequestBody DeleteMeetingByIdForm form){
        meetingService.deleteMeetingById(form.getId());
        return R.ok().put("result","success");
    }

    /**
     * 理论上要查询者要能有创建者的部门经理级别权限
     * 查询着应该是部门经理
     * @param form
     * @param token
     * @return
     */
    @PostMapping("/searchMeetingByManagerDept")
    @ApiOperation("根据不同条件查询会议")
    @RequiresPermissions(value = { "WORKFLOW:APPROVAL"}, logical = Logical.OR)//MEETING:CHECKIN
    public R searchMeetingByManagerDept(@Valid @RequestBody SearchMeetingByManagerDeptForm form
            , @RequestHeader("token") String token) {
        HashMap param = new HashMap();
        int userId = jwtUtil.getUserId(token);  // 理论上应该只有创建人和有删除权限的人才能操作吧
        int page = form.getPage();
        int length = form.getLength();
        long start = (page - 1) * length;

        param.put("start", start);
        param.put("length", length);
        ArrayList array = new ArrayList();
        if ("待审批".equals(form.getType())){
            array.add(1);   // 刚创建
        }else if ("已审批".equals(form.getType())){
            array.add(2);   // 审核未通过
            array.add(3);   // 未开始
            array.add(4);   // 进行中
            array.add(5);
            param.put("id", userId);
        }else {
            throw new EmosException("审批流程异常");
        }
        param.put("type", array);


        System.out.println("待审批前端传数据："+param);
        ArrayList<HashMap> list = meetingService.searchMeetingByManagerDept(param);

        log.info("result_list_meeting: {}", list);
        return R.ok().put("result", list);
    }

    @PostMapping("/approvalMeeting")
    @ApiOperation("审批未审批的会议")
    @RequiresPermissions(value = {"ROOT","MEETING:UPDATE","WORKFLOW:APPROVAL"}, logical = Logical.OR)
    public R approvalMeeting(@Valid @RequestBody ApprovalMeetingInfoForm form
            , @RequestHeader("token") String token) {
        int approvalId = jwtUtil.getUserId(token);

        int i = meetingService.approvalMeetingInfo(form.getFlag(), form.getId(),approvalId);
        if (i != 1)
            throw new EmosException("审批出问题了");
        return R.ok().put("result", "success");
    }

    @PostMapping("/receiveNotify")
    @ApiOperation("接受工作流通知")
    public R receiveNotify(@Valid @RequestBody ReceiveNotifyForm form){
        if (form.getResult().equals("同意")){
            log.debug(form.getUuid() + "的会议审批通过");
        }else{
            log.debug(form.getUuid() + "的会议审批不通过");
        }
        return R.ok();
    }

    @PostMapping("/searchRoomIdByUUID")
    @ApiOperation("查询会议房间RoomID")
    public R searchRoomIdByUUID(@Valid @RequestBody SearchRoomIdByUUIDForm form){
        long roomId=meetingService.searchRoomIdByUUID(form.getUuid());
        return R.ok().put("result",roomId);
    }

    @PostMapping("/searchUserMeetingInMonth")
    @ApiOperation("查询某月用户的会议日期列表")
    public R searchUserMeetingInMonth(@Valid @RequestBody SearchUserMeetingInMonthForm form,@RequestHeader("token") String token){
        int userId=jwtUtil.getUserId(token);
        HashMap param=new HashMap();
        param.put("userId",userId);
        param.put("express",form.getYear()+"/"+form.getMonth());
        List<String> list=meetingService.searchUserMeetingInMonth(param);
        return R.ok().put("result",list);
    }
}
