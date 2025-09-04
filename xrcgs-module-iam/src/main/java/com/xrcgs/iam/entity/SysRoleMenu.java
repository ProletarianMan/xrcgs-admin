package com.xrcgs.iam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;


/**
 * 用户菜单绑定
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu {
    @TableId(type = IdType.AUTO)
    private Long id;   // 复合主键不好做逻辑删，简化为自增ID

    private Long roleId;
    private Long menuId;
}
