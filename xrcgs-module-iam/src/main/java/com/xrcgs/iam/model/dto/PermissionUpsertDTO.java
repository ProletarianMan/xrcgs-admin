package com.xrcgs.iam.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 独立权限新增/修改 DTO
 */
@Data
public class PermissionUpsertDTO {

    private Long parentId;

    @NotBlank(message = "权限编码不能为空")
    @Size(max = 128, message = "权限编码长度不能超过128个字符")
    private String code;

    @NotBlank(message = "权限名称不能为空")
    @Size(max = 64, message = "权限名称长度不能超过64个字符")
    private String name;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    private String remark;

    @PositiveOrZero(message = "排序号不允许为负数")
    private Integer order;
}
