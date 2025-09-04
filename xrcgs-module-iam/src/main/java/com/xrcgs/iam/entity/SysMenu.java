package com.xrcgs.iam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.xrcgs.iam.enums.MenuType;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统菜单显示
 */
@Data
@TableName("sys_menu")
public class SysMenu {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;
    private String name;
    private String path;
    private String component;

    @TableField("type")
    private MenuType type; // DIR/MENU/BUTTON/API

    private String perms;   // 例：iam:user:list
    private String icon;
    private Integer orderNo;
    private Integer visible; // 1显示 0隐藏
    private Integer status;  // 1启用 0禁用

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;
}
