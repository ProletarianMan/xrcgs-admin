package com.xrcgs.iam.service;

import com.xrcgs.iam.entity.SysMenu;
import com.xrcgs.iam.model.query.MenuQuery;
import com.xrcgs.iam.model.vo.MenuRouteVO;
import com.xrcgs.iam.model.vo.MenuTreeVO;

import java.util.List;

public interface MenuService {
    Long create(SysMenu menu);
    void update(SysMenu menu);
    void remove(Long id);

    List<MenuTreeVO> treeAllEnabled();       // 全部启用态
    List<MenuTreeVO> treeByRole(Long roleId); // 指定角色
    List<MenuRouteVO> listByRoleCodes(List<String> roleCodes); // 通过角色编码集合
    List<SysMenu> list(MenuQuery q);
}
