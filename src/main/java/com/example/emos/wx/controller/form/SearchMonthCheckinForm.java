package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;
//接受前端提交数据
@Data
@ApiModel("用户月签到情况请求类")
public class SearchMonthCheckinForm {
    @NotNull
    @Range(min = 2000, max = 2100)
    private Integer year;

    @NotNull
    @Range(min = 1, max = 12)
    private Integer month;
}
