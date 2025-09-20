package com.xrcgs.iam.controller;

import com.xrcgs.common.core.R;
import com.xrcgs.iam.model.dto.DeptUpsertDTO;
import com.xrcgs.iam.model.vo.DeptTreeVO;
import com.xrcgs.iam.model.vo.DeptVO;
import com.xrcgs.iam.service.DeptService;
import com.xrcgs.syslog.annotation.OpLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理接口
 */
@Validated
@RestController
@RequestMapping("/api/iam/dept")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @GetMapping("/tree")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:dept:tree')")
    public R<List<DeptTreeVO>> tree(@RequestParam(value = "name", required = false) String name,
                                    @RequestParam(value = "status", required = false) Integer status) {
        return R.ok(deptService.tree(name, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:dept:list')")
    public R<DeptVO> detail(@PathVariable @NotNull Long id) {
        return R.ok(deptService.detail(id));
    }

    @PostMapping
    @OpLog("新增部门")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:dept:create')")
    public R<Long> create(@Valid @RequestBody DeptUpsertDTO dto) {
        return R.ok(deptService.create(dto));
    }

    @PutMapping("/{id}")
    @OpLog("修改部门")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:dept:update')")
    public R<Boolean> update(@PathVariable @NotNull Long id,
                             @Valid @RequestBody DeptUpsertDTO dto) {
        deptService.update(id, dto);
        return R.ok(true);
    }

    @DeleteMapping("/{id}")
    @OpLog("删除部门")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:dept:delete')")
    public R<Boolean> delete(@PathVariable @NotNull Long id) {
        deptService.delete(id);
        return R.ok(true);
    }
}
