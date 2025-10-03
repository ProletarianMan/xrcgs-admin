package com.xrcgs.iam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xrcgs.iam.enums.MenuType;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统菜单显示
 */
@Data
@TableName("sys_menu")
public class SysMenu {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long parentId;
    private String title;         // 菜单显示名称
    private String routerName;    // 前端路由名称
    private String path;
    private String component;

    @TableField("type")
    private MenuType type; // DIR/MENU/BUTTON/API

    private String perms;   // 例：iam:user:list
    private String icon;
    @TableField("`rank`")
    private Integer rank;
    @TableField("keep_alive")
    private Boolean keepAlive;    // 是否开启组件缓存
    @TableField("show_parent")
    private Boolean showParent;   // 面包屑中是否显示父级
    private Integer visible; // 1显示 0隐藏
    @TableField("`status`")
    private Integer status;  // 1启用 0禁用

    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;
}
