package com.xrcgs.iam.model.vo;

import lombok.Data;

/**
 * 菜单元数据
 */
@Data
public class MenuMetaVO {
    private String title;
    private String icon;
    private Integer rank;
    private Boolean keepAlive;
    private Boolean showParent;
}
