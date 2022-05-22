package com.example.emos.wx.db.mapper;

import com.example.emos.wx.db.pojo.TbWorkday;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

/**
* @author 20773
* @description 针对表【tb_workday】的数据库操作Mapper
* @createDate 2022-02-09 21:47:10
* @Entity com.example.emos.wx.db.pojo.TbWorkday
*/
@Mapper
public interface TbWorkdayMapper {
    public Integer searchTodayIsWorkday();
    //查询特殊调休工作日
    public ArrayList<String> searchWorkdayInRange(HashMap param);
}
