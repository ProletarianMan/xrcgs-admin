package com.xrcgs.iam.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xrcgs.iam.model.dto.RoleGrantMenuDTO;
import com.xrcgs.iam.model.dto.RoleGrantPermDTO;
import com.xrcgs.iam.model.dto.RoleUpsertDTO;
import com.xrcgs.iam.model.query.RolePageQuery;
import com.xrcgs.iam.model.vo.RolePageVO;
import com.xrcgs.iam.service.RoleService;
// ↓↓↓ 按你项目实际包名修改（任选其一/或替换为真实路径）
import com.xrcgs.common.core.R; // 如果你的 R 在这里
import com.xrcgs.syslog.annotation.OpLog; // 按你实际包名替换

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色控制
 */
@Validated
@RestController
@RequestMapping("/api/iam/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    // 分页查询
    @GetMapping("/page")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:role:list')")
    public R<Page<RolePageVO>> page(@Valid RolePageQuery q,
                                    @RequestParam(defaultValue = "1") long pageNo,
                                    @RequestParam(defaultValue = "10") long pageSize) {
        Page<RolePageVO> page = roleService.page(q, pageNo, pageSize);
        return R.ok(page);
    }

    // 新增
    @PostMapping
    @OpLog("新增角色")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:role:create')")
    public R<Long> create(@Valid @RequestBody RoleUpsertDTO dto) {
        dto.setId(null);
        Long id = roleService.upsert(dto);
        return R.ok(id);
    }

    // 修改
    @PutMapping("/{id}")
    @OpLog("修改角色")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:role:update')")
    public R<Long> update(@PathVariable @NotNull Long id,
                          @Valid @RequestBody RoleUpsertDTO dto) {
        dto.setId(id);
        Long rid = roleService.upsert(dto);
        return R.ok(rid);
    }

    // 删除
    @DeleteMapping("/{id}")
    @OpLog("删除角色")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:role:delete')")
    public R<Boolean> delete(@PathVariable @NotNull Long id) {
        roleService.remove(id);
        return R.ok(true);
    }

    // 角色拥有的菜单ID
    @GetMapping("/{id}/menu-ids")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:role:grantMenu')")
    public R<List<Long>> listRoleMenuIds(@PathVariable @NotNull Long id) {
        return R.ok(roleService.listMenuIdsByRole(id));
    }

    // 角色拥有的权限ID（独立权限表，可选）
    @GetMapping("/{id}/perm-ids")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:role:grantPerm')")
    public R<List<String>> listRolePermIds(@PathVariable @NotNull Long id) {
        return R.ok(roleService.listPermIdsByRole(id));
    }

    // 分配菜单
    @PostMapping("/grant-menus")
    @OpLog("角色授权菜单")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:role:grantMenu')")

    public R<Boolean> grantMenus(@Valid @RequestBody RoleGrantMenuDTO dto) {
        roleService.grantMenus(dto);
        return R.ok(true);
    }

    // 分配权限码（独立权限）
    @PostMapping("/grant-perms")
    @OpLog("角色授权权限码")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:role:grantPerm')")
    public R<Boolean> grantPerms(@Valid @RequestBody RoleGrantPermDTO dto) {
        roleService.grantPerms(dto);
        return R.ok(true);
    }
}
