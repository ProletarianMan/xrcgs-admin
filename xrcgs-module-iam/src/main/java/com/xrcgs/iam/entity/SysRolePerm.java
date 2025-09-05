package com.xrcgs.iam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 角色-权限关联表
 */
@Data
@TableName("sys_role_perm")
public class SysRolePerm {
    @TableId(type = IdType.ASSIGN_UUID)
    private Long id;

    private Long roleId;
    private Long permId;
}
