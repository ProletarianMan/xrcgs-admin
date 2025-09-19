package com.xrcgs.iam.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 部门新增/修改 DTO
 */
@Data
public class DeptUpsertDTO {

    /**
     * 父部门 ID，0 表示顶级
     */
    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 100, message = "部门名称长度不能超过100个字符")
    private String name;

    @Size(max = 100, message = "部门编码长度不能超过100个字符")
    private String code;

    private Integer status;

    private Integer sortNo;

    private Long leaderUserId;

    @Size(max = 30, message = "联系电话长度不能超过30个字符")
    private String phone;

    @Size(max = 100, message = "联系邮箱长度不能超过100个字符")
    private String email;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    private String remark;
}
