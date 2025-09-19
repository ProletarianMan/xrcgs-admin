package com.xrcgs.iam.datascope;

import com.xrcgs.iam.entity.SysDept;
import com.xrcgs.iam.entity.SysRole;
import com.xrcgs.iam.entity.SysUser;
import com.xrcgs.iam.enums.DataScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DataScopeCalculatorTest {

    private final DataScopeCalculator calculator = new DataScopeCalculator();

    @Test
    void shouldAggregateUserAndRoleScopes() {
        SysUser user = new SysUser();
        user.setDeptId(1L);
        user.setExtraDeptIds("[4]");
        user.setDataScope(DataScope.DEPT_AND_CHILD);

        SysRole customRole = new SysRole();
        customRole.setStatus(1);
        customRole.setDelFlag(0);
        customRole.setDataScope(DataScope.CUSTOM);
        customRole.setDataScopeExt("[3]");

        SysRole deptRole = new SysRole();
        deptRole.setStatus(1);
        deptRole.setDelFlag(0);
        deptRole.setDataScope(DataScope.DEPT);
        deptRole.setDeptId(2L);
        deptRole.setExtraDeptIds("[5]");

        SysRole selfRole = new SysRole();
        selfRole.setStatus(1);
        selfRole.setDelFlag(0);
        selfRole.setDataScope(DataScope.SELF);

        List<SysDept> departments = new ArrayList<>();
        departments.add(dept(1L, 0L));
        departments.add(dept(2L, 1L));
        departments.add(dept(3L, 1L));
        departments.add(dept(4L, 2L));
        departments.add(dept(5L, 4L));

        EffectiveDataScope scope = calculator.calculate(user, List.of(customRole, deptRole, selfRole), departments);

        assertFalse(scope.isAll());
        assertTrue(scope.isSelf());
        assertTrue(scope.getDeptIds().containsAll(Set.of(1L, 2L, 3L, 4L, 5L)));
    }

    @Test
    void shouldReturnAllWhenAnyScopeIsAll() {
        SysUser user = new SysUser();
        user.setDataScope(DataScope.SELF);

        SysRole allRole = new SysRole();
        allRole.setStatus(1);
        allRole.setDelFlag(0);
        allRole.setDataScope(DataScope.ALL);

        EffectiveDataScope scope = calculator.calculate(user, List.of(allRole), List.of());

        assertTrue(scope.isAll());
        assertTrue(scope.isSelf());
        assertTrue(scope.getDeptIds().isEmpty());
    }

    private SysDept dept(Long id, Long parentId) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setParentId(parentId);
        dept.setDelFlag(0);
        return dept;
    }
}
