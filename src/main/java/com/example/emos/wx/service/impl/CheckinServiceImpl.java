package com.example.emos.wx.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baidu.aip.face.MatchRequest;
import com.example.emos.wx.config.SystemConstants;
import com.example.emos.wx.db.mapper.*;
import com.example.emos.wx.db.pojo.TbCheckin;
import com.example.emos.wx.db.pojo.TbFaceModel;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.CheckinService;
import com.example.emos.wx.task.EmailTask;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import com.baidu.aip.face.AipFace;

import java.io.IOException;
import java.util.*;

//封装检测当天是否可以签到
@Service
@Scope("prototype")
@Slf4j
public class CheckinServiceImpl implements CheckinService {

    @Autowired
    private SystemConstants constants;
    @Autowired
    private TbHolidaysMapper holidaysMapper;
    @Autowired
    private TbWorkdayMapper workdayMapper;
    @Autowired
    private TbCheckinMapper checkinMapper;
    @Autowired
    private TbFaceModelMapper faceModelMapper;
    @Value("${emos.email.hr}")
    private String hrEmail;
    @Value("${emos.code}")
    private String code;
    @Autowired
    private EmailTask emailTask;
    @Autowired
    private TbUserMapper userMapper;
    @Autowired
    private TbCityMapper cityMapper;

    @Value("${emos.face.createFaceModelUrl}")
    private String createFaceModelUrl;

    @Value("${emos.face.checkinUrl}")
    private String checkinUrl;

    @Autowired
    private AipFace aipFace;
    private Object face_list;
    private Object i;

    @Override
    public String validCanCheckIn(int userId,String date) {
        // 返回结果不为空就说明是特殊节假日
        boolean bool_1 = holidaysMapper.searchTodayIsHolidays() != null;
        // 返回结果不为空就说明是特殊工作日
        boolean bool_2 = workdayMapper.searchTodayIsWorkday() != null;
        String type = "工作日";
        if (DateUtil.date().isWeekend()) {
            type = "节假日";
        }
        if (bool_1) {
            type = "节假日";
        } else if (bool_2) {
            type = "工作日";
        }

        if (type.equals("节假日")) {
            return "节假日无需考勤";
        } else {
            DateTime now = DateUtil.date();    // 当前时间
            // 本日打卡开始
            String start = DateUtil.today() + " " + constants.attendanceStartTime;
            // 本日结束打卡
            String end = DateUtil.today() + " " + constants.attendanceEndTime;
            DateTime attendanceStart = DateUtil.parse(start);
            DateTime attendanceEnd = DateUtil.parse(end);
            if (now.isBefore(attendanceStart)) {
                return "未到考勤时间";
            } else if (now.isAfter(attendanceEnd)) {
                return "超过考勤时间";
            } else {
                HashMap map = new HashMap();
                map.put("userId", userId);
                map.put("date", date);
                map.put("start", start);
                map.put("end", end);
                System.err.println("map ===  == >> " + JSONUtil.toJsonStr(map));
                boolean bool = checkinMapper.haveCheckin(map) != null;
                System.err.println("checkinDao.haveCheckin(map) ===  == >> " + JSONUtil.toJsonStr(checkinMapper.haveCheckin(map)));
                return bool ? "今日已经考勤" : "可以考勤";
            }
        }
    }

    @Override
    public void checkin(HashMap param) {
        //当前时间
        Date d1 = DateUtil.date();
        //上班时间
        Date d2 = DateUtil.parse(DateUtil.today() + " " + constants.attendanceTime);
        //签到结束时间
        Date d3 = DateUtil.parse(DateUtil.today() + " " + constants.attendanceEndTime);
        int status = 1;
        // 当前时间早于上班时间, 正常签到
        if (d1.compareTo(d2) <= 0) {
            status = 1;// 正常考勤
        } else if (d1.compareTo(d2) > 0 && d1.compareTo(d3) < 0) {
            status = 2;// 当前时间大于上班时间 并且小于考勤结束时间， 迟到
        }
        int userId = (Integer) param.get("userId");
        //判断当天是否已签到
/*        HashMap mapdate = searchTodayCheckin(userId);
        String todayhadcheckin =DateUtil.date((Date) mapdate.get("date")).toDateStr();

        System.out.println("DateUtil"+DateUtil.today());
        System.out.println("todayhadcheckin---->"+todayhadcheckin);
        if (Objects.equals(DateUtil.today(), todayhadcheckin)) {
            throw new EmosException("今天已经签过了");
        }*/
        //查询签到人的人脸模型数据
        String faceModel = faceModelMapper.searchFaceModel(userId);
        System.out.println("执行后");
        if (faceModel == null) {
            throw new EmosException("不存在人脸模型");
        } else {
            String path = (String) param.get("path");
            List<MatchRequest> requests = new ArrayList<>();
            requests.add(new MatchRequest(faceModel, "FACE_TOKEN"));
            String image = Base64.encode(FileUtil.file(path));
            detectFace(image, "BASE64");
            requests.add(new MatchRequest(image, "BASE64"));
            JSONObject resultJSON = aipFace.match(requests);
            Map<String, Object> resultMap = resultJSON.toMap();
            HashMap<String, Object> resultData = (HashMap<String, Object>) resultMap.get("result");

            // 获取相似度分数
            Double score = Double.valueOf(resultData.get("score").toString());
            System.out.println("score------"+score);
            if (score < 80) {
                throw new EmosException("签到无效，非本人签到");
            } else if (score > 81) {

           /* String path = (String) param.get("path");
            HttpRequest request = HttpUtil.createPost(checkinUrl);
            request.form("photo", FileUtil.file(path), "targetModel", faceModel);
            request.form("code",code);
            HttpResponse response = request.execute();

            System.out.println(response);//测试人脸识别返回结果

            if (response.getStatus() != 200) {
                log.error("人脸识别服务异常");
                throw new EmosException("人脸识别服务异常");
            }
            String body = response.body();
            if ("无法识别出人脸".equals(body) || "照片中存在多张人脸".equals(body)) {
                throw new EmosException(body);
            } else if ("False".equals(body)) {
                throw new EmosException("签到无效，非本人签到");
            } else if ("True".equals(body)) {*/
                // 这里要获取签到地区新冠疫情风险等级
                int risk = 1;//默认1:低风险,2:中风险,3:高风险
                String city = (String) param.get("city");
                String district = (String) param.get("district");
                String country = (String) param.get("country");
                String province = (String) param.get("province");
                String address = (String) param.get("address");
                if (!StrUtil.isBlank(city) && !StrUtil.isBlank(district)) {
                    String code = cityMapper.searchCode(city);
                    String url = "http://m." + code + ".bendibao.com/news/yqdengji/?qu=" + district;
                    // 查询风险地区
                    try {
                        //发出get类型http请求 获得html保存在document中
                        Document document = Jsoup.connect(url).get();//使用jsoup发送http请求
                        Elements elements = document.getElementsByClass("list-content");
                        if (elements.size() > 0) {
                            Element element = elements.get(0);
                            String result = element.select("p:last-child").text(); //获取最后一个p标签内容
//                            result = "高风险";
                            if ("高风险".equals(result)) {
                                risk = 3;
                                // 发送告警邮件
                                HashMap<String, String> map = userMapper.searchNameAndDept(userId);
                                String name = map.get("name");
                                String deptName = map.get("dept_name");
                                deptName = deptName != null ? deptName : "";
                                SimpleMailMessage message = new SimpleMailMessage();
                                message.setTo(hrEmail);
                                message.setSubject("员工" + name + "身处高风险疫情地区警告");
                                message.setText(deptName + "员工" + name + "， " + DateUtil.format(new Date(), "yyyy年MM月dd日") + "处于" + address + "，属于新冠疫情高风险地区，请及时与该员工联系，核实情况！");
                                // 异步多线程发送告警邮件 发送邮件跟保存签到数据没有前后关系
                                emailTask.sendAsync(message);
                            } else if ("中风险".equals(result)) {
                                risk = 2;
                            }
                        }
                    } catch (IOException e) {
                        log.error("执行异常", e);
                        throw new EmosException("获取风险等级失败");
                    }
                }
                // 保存签到记录
                TbCheckin entity = new TbCheckin();
                entity.setUserId(userId);
                entity.setAddress(address);
                entity.setCountry(country);
                entity.setProvince(province);
                entity.setCity(city);
                entity.setDistrict(district);
                entity.setStatus((byte) status);
                entity.setRisk(risk);
                entity.setDate(DateUtil.today());
                entity.setCreateTime(d1);
                checkinMapper.insert(entity);
            }
        }
    }

    /*人脸检测*/
    private void detectFace(String image,String imageType) {
        JSONObject detect = aipFace.detect(image, imageType,new HashMap<>());//人脸检测
        System.out.println("detect------"+detect);
        System.out.println("result------"+detect.get("result"));
/*        if ("null".equals(String.valueOf(detect.get("result")))) {
            throw new EmosException("未检测出人脸");
        }*/
        if (detect.isNull("result")) {
            throw new EmosException("未检测出人脸");
        }
/*        if ("pic not has face".equals(detect.get("error_msg"))) {
            throw new EmosException("未检测出人脸");
        }*/
        Map<String, Object> detectresult = detect.toMap();
        HashMap<String, Object> resultData1 = (HashMap<String, Object>) detectresult.get("result");

        List face_list = (List) resultData1.get("face_list");
        for (int i = 0; i < face_list.size(); i++) {
            HashMap o = (HashMap) face_list.get(i);
            Double face_probability = Double.parseDouble(o.get("face_probability").toString());
            System.out.println("------"+face_probability);
            if (face_probability<0.6) {
                throw new EmosException("无法识别出人脸");
            }
        }
        Integer face_num = (Integer) resultData1.get("face_num");
        System.out.println("------"+face_num);
        if (face_num > 1) {
            throw new EmosException("不能存在多张人脸");
        }
    }
    //创建人脸模型
    @Override
    public void createFaceModel(int userId, String path) {
        // 将图片转为Base64编码
        String image = Base64.encode(FileUtil.file(path));
        String imageType = "BASE64";
        detectFace(image,imageType);
        String groupId = "test";
        JSONObject result = aipFace.addUser(image, imageType, groupId, Integer.toString(userId), new HashMap<>());
        Map<String, Object> resultMap = result.toMap();
        HashMap<String, String> resultData = (HashMap<String, String>) resultMap.get("result");

        /*HttpRequest request = HttpUtil.createPost(createFaceModelUrl);//Post请求Python创建
        request.form("photo", FileUtil.file(path));
        request.form("code",code);
        HttpResponse response = request.execute();
        String body = response.body();

        System.out.println("response:"+response);

        if ("无法识别出人脸".equals(body) || "照片中存在多张人脸".equals(body)) {
            throw new EmosException(body);
        } else {*/
            // 保存人脸数据
            TbFaceModel entity = new TbFaceModel();
            entity.setUserId(userId);
//            entity.setFaceModel(body);
            entity.setFaceModel(resultData.get("face_token"));
            faceModelMapper.insert(entity);
//        }
    }

    /**
     * 查询当前用户当天考勤状况
     * @param userId 用户Id
     * @return
     */
    @Override
    public HashMap<String, Object> searchTodayCheckin(int userId) {
        return checkinMapper.searchTodayCheckin(userId);
    }

    /**
     * 查询当前用户考勤总天数
     * @param userId 用户Id
     * @return
     */
    @Override
    public long searchCheckinDays(int userId) {
        return checkinMapper.searchCheckinDays(userId);
    }

    /**
     * 查询当前用户周考勤情况
     * @param  param
     * @return
     */
    @Override
    public ArrayList<HashMap> searchWeekCheckin(HashMap param) {
        ArrayList<HashMap> checkinList = checkinMapper.searchWeekCheckin(param);//根据用户Id和限定时间段查询签到情况

        ArrayList holidays = holidaysMapper.searchHolidaysInRange(param);//根据时间段查询本周特殊节假日

        ArrayList workdays = workdayMapper.searchWorkdayInRange(param);// 查询本周特殊工作日

        // 本周的开始日期和结束日期
        DateTime startDate = DateUtil.parseDate(param.get("startDate").toString());
        DateTime endDate = DateUtil.parseDate(param.get("endDate").toString());


        DateRange range = DateUtil.range(startDate, endDate, DateField.DAY_OF_MONTH); // 生成本周7天的日期对象

        ArrayList<HashMap> list = new ArrayList<>(); // 返回对象

        range.forEach(item -> {
            // 判断当前是工作日还是假期日
            String date = item.toString("yyyy-MM-dd");
            String type = "工作日";

            if (item.isWeekend()) {
                type = "节假日";
            }

            if (holidays != null && holidays.contains(date)) {
                type = "节假日";
            } else if (workdays != null && workdays.contains(date)) {
                type = "工作日";
            }

            String status = "";
            //今天之前的工作日记为缺勤
            if ("工作日".equals(type) && DateUtil.compare(item, DateUtil.date()) <= 0) {

                status = "缺勤";
                boolean flag = false;

                for (HashMap<String, String> map : checkinList) {
                    if (map.containsValue(date)) {  // 限定时间内签到表包含date时间则去签到status
                        status = map.get("status");
                        flag = true;
                        break;
                    }
                }

                DateTime endTime = DateUtil.parse(DateUtil.today() + " " + constants.attendanceEndTime);//上班考勤截止时间
                String today = DateUtil.today();

                if (date.equals(today) && DateUtil.date().isBefore(endTime) && !flag) { //查询当天上班考勤截止时间前返回null 非缺勤
                    status = "";
                }
            }

            HashMap map = new HashMap();
            map.put("date", date);
            map.put("status", status);
            map.put("type", type);
            map.put("day", item.dayOfWeekEnum().toChinese("周"));//转换为中文格式：周一
            list.add(map);

        });
        return list;
    }

    //查询用户本月签到情况
    @Override
    public ArrayList<HashMap> searchMonthCheckin(HashMap param) {
        return this.searchWeekCheckin(param);
    }
}
