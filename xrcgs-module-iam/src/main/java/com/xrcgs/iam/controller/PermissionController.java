package com.xrcgs.iam.controller;

import com.xrcgs.common.core.R;
import com.xrcgs.iam.model.dto.PermissionUpsertDTO;
import com.xrcgs.iam.model.vo.PermissionVO;
import com.xrcgs.iam.service.PermissionService;
import com.xrcgs.syslog.annotation.OpLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/iam/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping("/list")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:role:grantPerm')")
    public R<List<PermissionVO>> list(@RequestParam(value = "name", required = false) String name) {
        return R.ok(permissionService.list(name));
    }

    @PostMapping
    @OpLog("新增独立权限")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:permission:create')")
    public R<Long> create(@Valid @RequestBody PermissionUpsertDTO dto) {
        return R.ok(permissionService.create(dto));
    }

    @PutMapping("/{id}")
    @OpLog("修改独立权限")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:permission:update')")
    public R<Boolean> update(@PathVariable @NotNull Long id, @Valid @RequestBody PermissionUpsertDTO dto) {
        permissionService.update(id, dto);
        return R.ok(true);
    }

    @DeleteMapping("/{id}")
    @OpLog("删除独立权限")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:permission:delete')")
    public R<Boolean> delete(@PathVariable @NotNull Long id) {
        permissionService.remove(id);
        return R.ok(true);
    }
}
