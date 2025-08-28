package com.xrcgs.auth.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 系统用户表
 */
@Data
@TableName("sys_user")
public class SysUser {
    @TableId
    private Long id;
    private String username;
    private String password;
    private Boolean enabled;
    private String nickname;
}
