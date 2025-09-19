package com.xrcgs.iam.datascope;

import com.xrcgs.iam.entity.SysDept;
import com.xrcgs.iam.entity.SysRole;
import com.xrcgs.iam.entity.SysUser;
import com.xrcgs.iam.enums.DataScope;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Combine user/role configuration into a single {@link EffectiveDataScope} instance.
 */
@Component
public class DataScopeCalculator {

    public EffectiveDataScope calculate(SysUser user, List<SysRole> roles, List<SysDept> departments) {
        if (user == null) {
            return EffectiveDataScope.selfOnly();
        }
        Map<Long, Set<Long>> children = DataScopeUtil.buildChildrenMap(departments);

        boolean all = false;
        boolean self = false;
        Set<Long> deptIds = new LinkedHashSet<>();

        DataScope userScope = user.getDataScope() == null ? DataScope.SELF : user.getDataScope();
        Set<Long> userExtraDepts = DataScopeUtil.parseIdSet(user.getExtraDeptIds());
        Set<Long> userCustomDepts = DataScopeUtil.parseIdSet(user.getDataScopeExt());

        switch (userScope) {
            case ALL -> all = true;
            case SELF -> self = true;
            case DEPT -> {
                addDept(user.getDeptId(), deptIds);
                DataScopeUtil.merge(deptIds, userExtraDepts);
            }
            case DEPT_AND_CHILD -> {
                deptIds.addAll(DataScopeUtil.collectWithChildren(user.getDeptId(), children));
                for (Long extra : userExtraDepts) {
                    deptIds.addAll(DataScopeUtil.collectWithChildren(extra, children));
                }
            }
            case CUSTOM -> DataScopeUtil.merge(deptIds, userCustomDepts);
        }

        if (!all && roles != null) {
            for (SysRole role : roles) {
                if (role == null) {
                    continue;
                }
                if (role.getDelFlag() != null && role.getDelFlag() == 1) {
                    continue;
                }
                if (role.getStatus() != null && role.getStatus() != 1) {
                    continue;
                }
                DataScope scope = role.getDataScope() == null ? DataScope.SELF : role.getDataScope();
                Set<Long> extraDepts = DataScopeUtil.parseIdSet(role.getExtraDeptIds());
                Set<Long> customDepts = DataScopeUtil.parseIdSet(role.getDataScopeExt());
                switch (scope) {
                    case ALL -> {
                        all = true;
                        self = true;
                    }
                    case SELF -> self = true;
                    case DEPT -> {
                        Long base = role.getDeptId() != null ? role.getDeptId() : user.getDeptId();
                        addDept(base, deptIds);
                        DataScopeUtil.merge(deptIds, extraDepts);
                    }
                    case DEPT_AND_CHILD -> {
                        Long base = role.getDeptId() != null ? role.getDeptId() : user.getDeptId();
                        deptIds.addAll(DataScopeUtil.collectWithChildren(base, children));
                        for (Long extra : extraDepts) {
                            deptIds.addAll(DataScopeUtil.collectWithChildren(extra, children));
                        }
                    }
                    case CUSTOM -> DataScopeUtil.merge(deptIds, customDepts);
                }
                if (all) {
                    break;
                }
            }
        }

        if (all) {
            return EffectiveDataScope.all();
        }

        if (!self && deptIds.isEmpty()) {
            self = true;
        }

        return EffectiveDataScope.ofDepartments(deptIds, self);
    }

    private void addDept(Long deptId, Collection<Long> target) {
        if (deptId != null && target != null) {
            target.add(deptId);
        }
    }
}
