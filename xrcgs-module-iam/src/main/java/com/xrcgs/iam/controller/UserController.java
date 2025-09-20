package com.xrcgs.iam.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xrcgs.common.core.R;
import com.xrcgs.iam.model.dto.UserUpsertDTO;
import com.xrcgs.iam.model.query.UserPageQuery;
import com.xrcgs.iam.model.vo.UserVO;
import com.xrcgs.iam.service.UserService;
import com.xrcgs.syslog.annotation.OpLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/iam/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/page")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:user:list')")
    public R<Page<UserVO>> page(@Valid UserPageQuery query,
                                @RequestParam(defaultValue = "1") long pageNo,
                                @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(userService.page(query, pageNo, pageSize));
    }

    @GetMapping("/search-by-nickname")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:user:list')")
    public R<List<UserVO>> searchByNickname(@RequestParam String nickname) {
        return R.ok(userService.listByNicknameSuffix(nickname));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:user:list')")
    public R<UserVO> detail(@PathVariable @NotNull Long id) {
        return R.ok(userService.detail(id));
    }

    @PostMapping
    @OpLog("新增用户")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:user:create')")
    public R<Long> create(@Valid @RequestBody UserUpsertDTO dto) {
        dto.setId(null);
        return R.ok(userService.create(dto));
    }

    @PutMapping("/{id}")
    @OpLog("修改用户")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:user:update')")
    public R<Boolean> update(@PathVariable @NotNull Long id,
                             @Valid @RequestBody UserUpsertDTO dto) {
        dto.setId(id);
        userService.update(id, dto);
        return R.ok(true);
    }

    @DeleteMapping("/{id}")
    @OpLog("删除用户")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:user:delete')")
    public R<Boolean> delete(@PathVariable @NotNull Long id) {
        userService.delete(id);
        return R.ok(true);
    }

    @PutMapping("/{id}/enable")
    @OpLog("启用用户")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:user:enable')")
    public R<Boolean> enable(@PathVariable @NotNull Long id) {
        userService.updateEnabled(id, true);
        return R.ok(true);
    }

    @PutMapping("/{id}/disable")
    @OpLog("停用用户")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:user:disable')")
    public R<Boolean> disable(@PathVariable @NotNull Long id) {
        userService.updateEnabled(id, false);
        return R.ok(true);
    }
}

