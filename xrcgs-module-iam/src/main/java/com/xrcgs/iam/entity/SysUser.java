package com.xrcgs.iam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xrcgs.iam.enums.DataScope;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String nickname;
    @TableField("wechat_id")
    private String wechatId;
    @TableField("phone")
    private String phone;
    @TableField("gender")
    private Integer gender;
    private Boolean enabled;

    private Long deptId;

    @TableField("extra_dept_ids")
    private String extraDeptIds;

    @TableField("data_scope")
    private DataScope dataScope;

    @TableField("data_scope_ext")
    private String dataScopeExt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
