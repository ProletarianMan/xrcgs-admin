package com.xrcgs.iam.controller;

import com.xrcgs.iam.entity.SysMenu;
import com.xrcgs.iam.model.query.MenuQuery;
import com.xrcgs.iam.model.vo.MenuRouteVO;
import com.xrcgs.iam.model.vo.MenuTreeVO;
import com.xrcgs.iam.service.MenuService;
// ↓↓↓ 按你项目实际包名修改
import com.xrcgs.common.core.R;

import com.xrcgs.syslog.annotation.OpLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单操作
 * iam:menu:list|tree|create|update|delete
 */
@Validated
@RestController
@RequestMapping("/api/iam/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    // 列表（支持条件查询）
    @GetMapping("/list")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:menu:list')")
    public R<List<SysMenu>> list(@Valid MenuQuery q) {
        return R.ok(menuService.list(q));
    }

    // 全量启用态菜单树（前端构建路由用）
    @GetMapping("/tree/all")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:menu:tree')")
    public R<List<MenuTreeVO>> treeAllEnabled() {
        return R.ok(menuService.treeAllEnabled());
    }

    // 指定角色的菜单树(不应该启用权限，登陆时需要拉去)
    @GetMapping("/tree/{roleId}")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:menu:tree')")
    public R<List<MenuTreeVO>> treeByRole(@PathVariable @NotNull Long roleId) {
        return R.ok(menuService.treeByRole(roleId));
    }

    // 根据角色编码集合获取菜单列表（平铺）
    @PostMapping("/tree/by-codes")
    public List<MenuRouteVO> listByRoleCodes(@RequestBody List<String> roleCodes) {
        return menuService.listByRoleCodes(roleCodes);
    }

    /**
     * 新增菜单
     * 支持接收 routerName、keepAlive、showParent 等前端路由字段
     */
    @PostMapping
    @OpLog("新增菜单")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:menu:create')")
    public R<Long> create(@Valid @RequestBody SysMenu menu) {
        Long id = menuService.create(menu);
        return R.ok(id);
    }

    /**
     * 修改菜单
     * 支持接收 routerName、keepAlive、showParent 等前端路由字段
     */
    @PutMapping("/{id}")
    @OpLog("修改菜单")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:menu:update')")
    public R<Boolean> update(@PathVariable @NotNull Long id,
                             @Valid @RequestBody SysMenu menu) {
        menu.setId(id);
        menuService.update(menu);
        return R.ok(true);
    }

    // 删除菜单（无子节点才能删）
    @DeleteMapping("/{id}")
    @OpLog("删除菜单")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:menu:delete')")
    public R<Boolean> delete(@PathVariable @NotNull Long id) {
        menuService.remove(id);
        return R.ok(true);
    }
}
