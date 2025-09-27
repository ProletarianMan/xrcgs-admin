package com.xrcgs.iam.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 用户分配角色请求体
 */
@Data
public class UserAssignRoleDTO {

    /**
     * 角色 ID 列表，允许为空列表表示清空角色
     */
    @NotNull(message = "角色列表不能为空")
    private List<Long> roleIds;
}
