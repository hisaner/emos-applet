package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

//封装客户端提交的数据。
@Data
@ApiModel
public class LoginForm {
    @NotBlank(message = "临时授权不能为空")
    private String code;

}
