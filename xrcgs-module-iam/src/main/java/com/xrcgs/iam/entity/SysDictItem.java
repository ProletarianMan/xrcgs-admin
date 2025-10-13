package com.xrcgs.iam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xrcgs.iam.model.vo.DeptBriefVO;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典项表
 */
@Data
@TableName("sys_dict_item")
public class SysDictItem {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String typeCode; // 外键到 sys_dict_type.code
    private String label;
    private String value;
    private Integer sort;
    private String ext;
    private Integer status; // 1启用 0禁用

    private Long deptId; // 归属部门

    @TableField(exist = false)
    private DeptBriefVO dept;

    @JsonFormat(pattern = "yyy-MM-dd")
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyy-MM-dd")
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
