package com.example.emos.wx.db.mapper;

import com.example.emos.wx.db.pojo.TbCity;
import org.apache.ibatis.annotations.Mapper;

/**
* @author 20773
* @description 针对表【tb_city(疫情城市列表)】的数据库操作Mapper
* @createDate 2022-02-09 21:47:10
* @Entity com.example.emos.wx.db.pojo.TbCity
*/
@Mapper
public interface TbCityMapper {
    public String searchCode(String city);


}
