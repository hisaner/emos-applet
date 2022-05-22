package com.example.emos.wx.db.mapper;

import com.example.emos.wx.db.pojo.TbCheckin;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

/**
* @author 20773
* @description 针对表【tb_checkin(签到表)】的数据库操作Mapper
* @createDate 2022-02-09 21:47:10
* @Entity com.example.emos.wx.db.pojo.TbCheckin
*/
@Mapper
public interface TbCheckinMapper {
    public Integer haveCheckin(HashMap param);//查询当天是否签到

    public void insert(TbCheckin checkin);
    //查询员工当天签到情况、员工请考勤日期总数
    public HashMap searchTodayCheckin(int userId);
    public long searchCheckinDays(int userId);
    public ArrayList<HashMap> searchWeekCheckin(HashMap param);

    //员工管理中删除员工的签到记录
    public int deleteUserCheckin(int userId);
}
