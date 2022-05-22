package com.example.emos.wx.db.mapper;

import com.example.emos.wx.db.pojo.TbHolidays;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

/**
* @author 20773
* @description 针对表【tb_holidays(节假日表)】的数据库操作Mapper
* @createDate 2022-02-09 21:47:10
* @Entity com.example.emos.wx.db.pojo.TbHolidays
*/
@Mapper
public interface TbHolidaysMapper {
    public Integer searchTodayIsHolidays();
    //查询特殊节假日
    public ArrayList<String> searchHolidaysInRange(HashMap param);

}
