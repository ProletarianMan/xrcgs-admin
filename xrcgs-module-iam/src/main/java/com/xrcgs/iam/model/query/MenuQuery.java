package com.xrcgs.iam.model.query;

import com.xrcgs.iam.enums.MenuType;
import lombok.Data;

/**
 * 数据模型
 * 菜单查询条件
 */
@Data
public class MenuQuery {
    private String keyword;   // 按 title/path/perms 模糊
    private Integer status;   // 1/0
    private MenuType type;    // 可选过滤：DIR/MENU/BUTTON/API
    private Long parentId;    // 查某父节点下
    private Integer visible;  // 1/0
}
