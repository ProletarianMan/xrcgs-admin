package com.xrcgs.iam.model.vo;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 *  表现层对象
 *  原生概念属性
 */
@Data
public class MenuTreeVO {
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String component;
    private String type;     // DIR/MENU/BUTTON/API
    private String icon;
    private Integer orderNo;
    private Integer visible;
    private Integer status;
    private String perms;    // 例：iam:user:list
    private List<MenuTreeVO> children = new ArrayList<>();
}
