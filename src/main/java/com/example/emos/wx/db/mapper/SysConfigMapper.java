package com.example.emos.wx.db.mapper;

import com.example.emos.wx.db.pojo.SysConfig;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* @author 20773
* @description 针对表【sys_config】的数据库操作Mapper
* @createDate 2022-02-09 21:47:10
* @Entity com.example.emos.wx.db.pojo.SysConfig
*/
@Mapper
public interface SysConfigMapper {
    public List<SysConfig> selectAllParam();
}
