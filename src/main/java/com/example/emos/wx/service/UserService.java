package com.example.emos.wx.service;

import cn.hutool.json.JSONObject;
import com.example.emos.wx.db.pojo.TbUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public interface UserService {
    public int registerUser(String registerCode, String code, String nickname, String photo);

    public Set<String> searchUserPermissions(int userId);

    public Integer login(String code);
    public TbUser searchById(int userId);

    public String searchUserHiredate(int userId);//查询员工的入职日期
    public HashMap searchUserSummary(int userId);//用户界面 查询用户概要信息

    ArrayList<HashMap> searchUserGroupByDept(String keyword);//查询员工数据

    ArrayList<HashMap> searchMembers(List param);

    public List<HashMap> selectUserPhotoAndName(List param);

    public String searchMemberEmail(int id);

    public void insertUser(HashMap param);

    public HashMap searchUserInfo(int userId);

    public int updateUserInfo(HashMap param);

    public void deleteUserById(int id);

    //通讯录
    public JSONObject searchUserContactList();
}
