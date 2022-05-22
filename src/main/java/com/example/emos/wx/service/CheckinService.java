package com.example.emos.wx.service;

import java.util.ArrayList;
import java.util.HashMap;

public interface CheckinService {
    /**
     * 查看用户今天是否可以签到
     * @param userId 用户ID
     * @param date   时间
     * @return 能否签到的信息
     */
    String validCanCheckIn(int userId, String date);

    /**
     * 执行签到
     * @param param
     */
    void checkin(HashMap param);

    /**
     * 创建人脸模型
     * @param userId 用户id
     * @param path   签到图片保存路径
     */
    void createFaceModel(int userId, String path);

    /**
     * 查询用户当天签到
     * @param userId 用户id
     * @return 当天签到记录
     */
    HashMap<String, Object> searchTodayCheckin(int userId);

    /**
     * 查询用户签到统计
     * @param userId 用户id
     * @return 签到天数统计
     */
    long searchCheckinDays(int userId);

    /**
     * 查询用户本周签到情况
     * @param HashMap userId startDate endDate
     * @return 本周签到记录
     */
    ArrayList<HashMap> searchWeekCheckin(HashMap param);

    /**
     * 查询用户本月签到情况
     * @param HashMap userId startDate endDate
     * @return 本月签到记录
     */
    ArrayList<HashMap> searchMonthCheckin(HashMap param);

}
