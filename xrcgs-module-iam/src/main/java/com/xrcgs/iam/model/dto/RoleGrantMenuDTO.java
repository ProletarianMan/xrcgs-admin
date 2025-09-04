package com.xrcgs.iam.model.dto;

import lombok.Data;
import java.util.List;

/**
 * 角色对应菜单项，数据传输
 */
@Data
public class RoleGrantMenuDTO {
    private Long roleId;
    private List<Long> menuIds;
}
