package com.xrcgs.iam.controller;

import com.xrcgs.common.core.R;
import com.xrcgs.iam.model.vo.PermissionVO;
import com.xrcgs.iam.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/iam/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping("/list")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:role:grantPerm')")
    public R<List<PermissionVO>> listAll() {
        return R.ok(permissionService.listAll());
    }
}
