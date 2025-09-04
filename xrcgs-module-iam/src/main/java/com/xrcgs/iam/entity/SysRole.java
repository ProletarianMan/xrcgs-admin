package com.xrcgs.iam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.xrcgs.iam.enums.DataScope;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 角色类型表
 */
@Data
@TableName("sys_role")
public class SysRole {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private Integer status;

    @TableField("data_scope")
    private DataScope dataScope;

    /** CUSTOM 时存部门ID数组 JSON（如 [1,2,3]） */
    private String dataScopeExt;

    private String remark;
    private Integer sortNo;
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 非物理删除
    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;
}
