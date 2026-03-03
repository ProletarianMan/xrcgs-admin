package com.xrcgs.iam.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xrcgs.common.core.R;
import com.xrcgs.iam.model.dto.UserAssignRoleDTO;
import com.xrcgs.iam.model.dto.UserResetPasswordDTO;
import com.xrcgs.iam.model.dto.UserUpsertDTO;
import com.xrcgs.iam.model.query.UserPageQuery;
import com.xrcgs.iam.model.vo.UserSimpleVO;
import com.xrcgs.iam.model.vo.UserVO;
import com.xrcgs.iam.service.UserService;
import com.xrcgs.syslog.annotation.OpLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

    /**
     * 获取传入用户名的同部门用户
     * 0：当前部门（与原接口一致，但强制确保结果含本人）
     * >0：向上找 N 级部门，然后返回该部门及其下属所有部门中的启用用户
     * 超过顶级时，自动回退到顶级部门范围
     * <0：向下找 N 级部门（按层级），返回该层级所有部门中的启用用户
     * 超过最低层时，自动回退到最低可达层级部门范围
     * 无论本人是否在上述“启用用户查询结果”里，最终结果都会补上本人（去重）
     * @param username 登录用户用户名
     * @param deptLevelOffset 上下级寻找
     * @return 用户
     */
    @GetMapping("/same-dept-users")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:user:list')")
    public R<List<UserSimpleVO>> sameDeptUsers(@RequestParam @NotBlank String username,
                                               @RequestParam(defaultValue = "0") Integer deptLevelOffset) {
        return R.ok(userService.listEnabledUsersInSameDept(username, deptLevelOffset));
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

    @PutMapping("/{id}/password")
    @OpLog("重置用户密码")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:user:update')")
    public R<Boolean> resetPassword(@PathVariable @NotNull Long id,
                                    @Valid @RequestBody UserResetPasswordDTO dto) {
        userService.resetPassword(id, dto.getPassword());
        return R.ok(true);
    }

    @PutMapping("/{id}/roles")
    @OpLog("分配用户角色")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:user:update')")
    public R<Boolean> assignRoles(@PathVariable @NotNull Long id,
                                  @Valid @RequestBody UserAssignRoleDTO dto) {
        userService.assignRoles(id, dto.getRoleIds());
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
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:user:update')")
    public R<Boolean> enable(@PathVariable @NotNull Long id) {
        userService.updateEnabled(id, true);
        return R.ok(true);
    }

    @PutMapping("/{id}/disable")
    @OpLog("停用用户")
    @PreAuthorize("@permChecker.hasPerm(authentication, 'iam:user:update')")
    public R<Boolean> disable(@PathVariable @NotNull Long id) {
        userService.updateEnabled(id, false);
        return R.ok(true);
    }
}

