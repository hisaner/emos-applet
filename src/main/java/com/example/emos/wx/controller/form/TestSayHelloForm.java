package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
//后端数据验证功能
@ApiModel
@Data
public class TestSayHelloForm {
/*    @NotBlank
    //只能输入2-15个汉字
    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{2,15}$")*/
    @ApiModelProperty("姓名")
    private String name;
}
