package com.xrcgs.iam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 字典类型表
 */
@Data
@TableName("sys_dict_type")
public class SysDictType {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;   // 唯一
    private String name;
    private Integer status; // 1启用 0禁用
    private String remark;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
