package com.xrcgs.iam.model.vo;

import lombok.Data;

/**
 * 菜单元数据
 */
@Data
public class MenuMetaVO {
    private String title;        // 菜单标题
    private String icon;         // 图标
    private Integer rank;        // 排序
    private Boolean keepAlive;   // 组件是否缓存
    private Boolean showParent;  // 是否显示父级
}
