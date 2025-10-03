package com.xrcgs.iam.entity;

import com.baomidou.mybatisplus.annotation.*;
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

    private String code;   // 唯一，如 file:doc:convert
    private String name;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
