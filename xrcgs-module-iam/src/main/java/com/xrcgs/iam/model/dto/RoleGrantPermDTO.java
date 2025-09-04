package com.xrcgs.iam.model.dto;

import lombok.Data;
import java.util.List;

/**
 * 角色对应独立权限，数据传输
 */
@Data
public class RoleGrantPermDTO {
    private Long roleId;
    private List<Long> permIds;
}
