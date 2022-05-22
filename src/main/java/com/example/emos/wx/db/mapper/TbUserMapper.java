package com.example.emos.wx.db.mapper;

import cn.hutool.json.JSONObject;
import com.example.emos.wx.db.pojo.TbUser;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
* @author 20773
* @description 针对表【tb_user(用户表)】的数据库操作Mapper
* @createDate 2022-02-09 21:47:10
* @Entity com.example.emos.wx.db.pojo.TbUser
*/
@Mapper
public interface TbUserMapper {
    public boolean haveRootUser();
    public int insert(HashMap param);
    public Integer searchIdByOpenId(String openId);

    public Set<String> searchUserPermissions(int userId);

    public TbUser searchById(int userId);

    public HashMap searchNameAndDept(int userId);

    public String searchUserHiredate(int userId);//查询员工的入职日期

    public HashMap searchUserSummary(int userId);//用户界面 查询用户概要信息

    ArrayList<HashMap> searchUserGroupByDept(String keyword);//查询员工数据

    ArrayList<HashMap> searchMembers(List param);


    HashMap searchUserInfo(int userId);

    int searchDeptManagerId(int id);

    int searchGmId();

    public List<HashMap> selectUserPhotoAndName(List param);

    public String searchMemberEmail(int id);

    public long searchUserCountInDept(int deptId);

    public int searchUserIdByEmail(String email);

    public int activeUserAccount(HashMap param);

    public int updateUserInfo(HashMap param);

    public int deleteUserById(int id);

    public ArrayList<HashMap> searchUserContactList();

}
