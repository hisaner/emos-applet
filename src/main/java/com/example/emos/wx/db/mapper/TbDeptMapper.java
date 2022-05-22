package com.example.emos.wx.db.mapper;

import com.example.emos.wx.db.pojo.TbDept;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
* @author 20773
* @description 针对表【tb_dept】的数据库操作Mapper
* @createDate 2022-02-09 21:47:10
* @Entity com.example.emos.wx.db.pojo.TbDept
*/
@Mapper
public interface TbDeptMapper {
    public ArrayList<HashMap> searchDeptMembers(String keyword);//查询部门成员


    public List<TbDept> searchAllDept();

    public int insertDept(String deptName);

    public int deleteDeptById(int id);

    public int updateDeptById(TbDept entity);
}
