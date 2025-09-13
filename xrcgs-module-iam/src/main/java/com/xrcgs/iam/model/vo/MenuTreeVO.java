package com.xrcgs.iam.model.vo;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 *  表现层对象
 *  路由树节点
 */
@Data
public class MenuTreeVO {
    private String path;
    private String name;
    private String component;
    private MenuMetaVO meta;
    private List<MenuTreeVO> children = new ArrayList<>();
}
