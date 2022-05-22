package com.example.emos.wx.config.xss;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.json.JSONUtil;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
//抵御跨站脚本攻击XSS
//对传入内容进行转义
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        String value= super.getParameter(name);
        if (!StrUtil.hasEmpty(value)){//使用Hutool工具类中的StrUtil.hasEmpty判断字符串是否为空
            value=HtmlUtil.filter(value);//转义字符串
        }
        return value;
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values= super.getParameterValues(name);
        if (values!=null){
            for (int i=0;i<values.length;i++){
                String value=values[i];
                if (!StrUtil.hasEmpty(value)){
                    value=HtmlUtil.filter(value);
                }
                values[i]=value;
            }
        }
        return values;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> parameters = super.getParameterMap();
        //创建新Map以存放转义处理后的数据
        LinkedHashMap<String, String[]> map = new LinkedHashMap<>();
        if (parameters!=null){
            for (String key:parameters.keySet()){
                String[] values=parameters.get(key);
                for (int i=0;i<values.length;i++){
                    String value=values[i];
                    if (!StrUtil.hasEmpty(value)){
                        value=HtmlUtil.filter(value);
                    }
                    values[i]=value;
                }
                map.put(key,values);
            }
        }
        return map;
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        if (!StrUtil.hasEmpty(value)){
            value=HtmlUtil.filter(value);
        }
        return value;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        InputStream in = super.getInputStream();
        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        //缓冲流
        BufferedReader buffer = new BufferedReader(reader);
        //拼接字符串
        StringBuffer body=new StringBuffer();//json格式
        //读取第一行
        String line=buffer.readLine();
        while (line!=null){
            body.append(line);
            line=buffer.readLine();
        }
        //关闭缓冲IO流
        buffer.close();
        reader.close();
        in.close();
        Map<String, Object> map = JSONUtil.parseObj(body.toString());//stringbuffer转字符串再转Map对象
        Map<String, Object> result = new LinkedHashMap<>();//新的LinkedHashMap实现类
        for (String key:map.keySet()){
            Object val=map.get(key);
            if (val instanceof String){
                if (!StrUtil.hasEmpty(val.toString())){
                    result.put(key,HtmlUtil.filter(val.toString()));
                }
            }
            else {
                result.put(key,val);
            }
        }
        String json = JSONUtil.toJsonStr(result);//把map类型的result转为json格式字符串
        ByteArrayInputStream bain = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));//创建新的IO流取数据
        return new ServletInputStream() {//使用匿名内部类返回ServletInputStream类型数据
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }

            @Override
            public int read() throws IOException {
                return bain.read();//只需重写read方法读取
            }
        };
    }
}
