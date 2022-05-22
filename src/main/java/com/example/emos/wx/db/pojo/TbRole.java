package com.example.emos.wx.db.pojo;

import java.io.Serializable;
import lombok.Data;

/**
 * 角色表
 * @TableName tb_role
 */
@Data
public class TbRole implements Serializable {
    /**
     * 主键
     */
    private Integer id;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 权限集合
     */
    private Object permissions;

    /**
     * '系统角色内置权限'
     */
    private String defaultPermissions;

    /**
     * 是否为系统内置角色
     */
    private Boolean systemic;
    /**
     * '描述'
     */
    private String desc;

    private static final long serialVersionUID = 1L;
}