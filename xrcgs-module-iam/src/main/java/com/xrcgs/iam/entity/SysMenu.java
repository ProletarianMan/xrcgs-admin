package com.xrcgs.iam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xrcgs.iam.enums.MenuType;
import com.xrcgs.iam.handler.BooleanToSmallIntTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统菜单
 */
@Data
@TableName(value = "sys_menu", autoResultMap = true)
public class SysMenu {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long parentId;
    private String title;
    private String routerName;
    private String path;
    private String component;

    @TableField("type")
    private MenuType type; // DIR/MENU/BUTTON/API

    private String perms;
    private String icon;

    @TableField("\"rank\"")
    private Integer rank;

    // PostgreSQL schema uses int2 for boolean-like columns.
    @TableField(value = "keep_alive", typeHandler = BooleanToSmallIntTypeHandler.class)
    private Boolean keepAlive;

    @TableField(value = "show_parent", typeHandler = BooleanToSmallIntTypeHandler.class)
    private Boolean showParent;

    private Integer visible; // 1: visible, 0: hidden

    @TableField("status")
    private Integer status;  // 1: enabled, 0: disabled

    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;
}
