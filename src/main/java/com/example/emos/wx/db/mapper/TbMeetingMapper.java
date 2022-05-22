package com.example.emos.wx.db.mapper;

import com.example.emos.wx.db.pojo.TbMeeting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
* @author 20773
* @description 针对表【tb_meeting(会议表)】的数据库操作Mapper
* @createDate 2022-02-09 21:47:10
* @Entity com.example.emos.wx.db.pojo.TbMeeting
*/
//会议status 1是刚创建 2是审核未通过 3是审核通过但未开始 4是正在进行中 5是结束了 6是已删除
@Mapper
public interface TbMeetingMapper {
    //添加会议
    public int insertMeeting(TbMeeting entity);

    //查询会议列表分页
    public ArrayList<HashMap> searchMyMeetingListByPage(HashMap param);

    boolean searchMeetingMembersInSameDept(String uuid);


    int updateMeetingInstanceId(HashMap map);//暂时

    public HashMap searchMeetingById(int id);

    public ArrayList<HashMap> searchMeetingMembers(int id);

    public int updateMeetingInfo(HashMap param);

    public int deleteMeetingById(int id);

    public List<String> searchUserMeetingInMonth(HashMap param);


    ArrayList<HashMap> searchMeetingByManagerDept(HashMap map);
    int updateMeetingFailById(int id,int approvalId);

    int updateMeetingSuccessById(@Param("id") int id, @Param("approvalId") int approvalId);

    int updateMeetingOver(int id);
    //逻辑删除 不物理删除了
    int updateMeetingToDelete(int id);

    int deleteMeetingInfo(int id);

    int updateMeetingToStarting(int id);
}
