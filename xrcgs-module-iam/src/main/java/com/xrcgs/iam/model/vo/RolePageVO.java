package com.xrcgs.iam.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xrcgs.iam.enums.DataScope;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色分页展示信息
 */
@Data
public class RolePageVO {

    private Long id;
    private String code;
    private String name;
    private Integer status;
    private Long deptId;
    private DeptBriefVO dept;
    private String extraDeptIds;
    private DataScope dataScope;
    private String dataScopeExt;
    private String remark;
    private Integer sortNo;
    private Long createBy;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime createTime;

    private Long updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime updateTime;

    private Integer delFlag;
}
