package com.xrcgs.syslog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xrcgs.common.core.R;
import com.xrcgs.syslog.annotation.OpLog;
import com.xrcgs.syslog.entity.SysOpLog;
import com.xrcgs.syslog.model.query.SysOpLogPageQuery;
import com.xrcgs.syslog.service.SysOpLogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 操作日志接口
 */
@Validated
@RestController
@RequestMapping("/api/syslog/op-log")
@RequiredArgsConstructor
public class SysOpLogController {

    private final SysOpLogService sysOpLogService;

    /**
     * 分页查询操作日志
     */
    @GetMapping("/page")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'sys:op-log:list')")
    public R<Page<SysOpLog>> page(@Valid SysOpLogPageQuery query,
                                  @RequestParam(defaultValue = "1") long pageNo,
                                  @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(sysOpLogService.page(query, pageNo, pageSize));
    }

    /**
     * 查询操作日志详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'sys:op-log:get')")
    public R<SysOpLog> get(@PathVariable @NotNull Long id) {
        return R.ok(sysOpLogService.get(id));
    }

    /**
     * 批量删除操作日志
     */
    @DeleteMapping
    @OpLog("删除操作日志")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'sys:op-log:delete')")
    public R<Boolean> remove(@RequestBody @NotEmpty List<@NotNull Long> ids) {
        return R.ok(sysOpLogService.deleteByIds(ids));
    }

    /**
     * 清空所有操作日志
     */
    @DeleteMapping("/all")
    @OpLog("清空操作日志")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'sys:op-log:delete')")
    public R<Integer> clearAll() {
        return R.ok(sysOpLogService.clearAll());
    }
}
