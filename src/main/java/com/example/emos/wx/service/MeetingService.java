package com.example.emos.wx.service;

import com.example.emos.wx.db.pojo.TbMeeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface MeetingService {
    //添加会议
    public void insertMeeting(TbMeeting entity);


     //查询会议列表分页
    public ArrayList<HashMap> searchMyMeetingListByPage(HashMap param);



    public HashMap searchMeetingById(int id);

    public void updateMeetingInfo(HashMap param);

    public void deleteMeetingById(int id);//deleteMeetingInfo

    public Long searchRoomIdByUUID(String uuid);//视频会议id

    public List<String> searchUserMeetingInMonth(HashMap param);

    public ArrayList<HashMap> searchMeetingByManagerDept(HashMap param);

    public int approvalMeetingInfo(int flag, int id,int approvalId);
}
