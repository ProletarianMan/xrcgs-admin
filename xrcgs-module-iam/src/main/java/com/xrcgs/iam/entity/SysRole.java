package com.xrcgs.iam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xrcgs.iam.enums.DataScope;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 角色类型表
 */
@Data
@TableName("sys_role")
public class SysRole {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String code;
    private String name;
    private Integer status;

    private Long deptId;

    @TableField("extra_dept_ids")
    private String extraDeptIds;

    @TableField("data_scope")
    private DataScope dataScope;

    /** CUSTOM 时存部门ID数组 JSON（如 [1,2,3]） */
    @TableField("data_scope_ext")
    private String dataScopeExt;

    private String remark;
    private Integer sortNo;
    private Long createBy;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 非物理删除
    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;
}
