package com.xrcgs.iam.model.vo;

import lombok.Data;

/**
 * 平铺菜单路由信息
 */
@Data
public class MenuRouteVO {
    private Long id;
    private Long parentId;
    private String path;
    private String name;
    private String component;
    private String title;
    private String icon;
    private Integer rank;
    private Boolean keepAlive;
    private Boolean showParent;
}
