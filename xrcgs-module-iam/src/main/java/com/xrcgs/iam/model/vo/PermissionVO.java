package com.xrcgs.iam.model.vo;

import lombok.Data;

/**
 * 独立权限返回对象
 */
@Data
public class PermissionVO {
    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private String remark;
    private Integer order;
}
