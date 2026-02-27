package com.xrcgs.iam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 独立权限
 */
@Data
@TableName("sys_permission")
public class SysPermission {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long parentId;
    private String code;
    private String name;
    private String remark;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
