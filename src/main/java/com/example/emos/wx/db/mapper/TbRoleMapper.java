package com.example.emos.wx.db.mapper;

import com.example.emos.wx.db.pojo.TbRole;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
* @author 20773
* @description 针对表【tb_role(角色表)】的数据库操作Mapper
* @createDate 2022-02-09 21:47:10
* @Entity com.example.emos.wx.db.pojo.TbRole
*/
@Mapper
public interface TbRoleMapper {

    public ArrayList<HashMap> searchRoleOwnPermission(int id);
    public List<TbRole> searchAllRole();
    public long searchRoleUsersCount(int id);
    public int insertRole(TbRole role);
    public int updateRolePermissions(TbRole role);
    public ArrayList<HashMap> searchAllPermission();
    public int deleteRoleById(int id);

}
