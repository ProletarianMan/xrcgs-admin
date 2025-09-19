package com.xrcgs.iam.datascope;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataScopeUtilApplyTest {

    private static final class DummyEntity {
        private Long createdBy;
        private Long deptId;

        public Long getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(Long createdBy) {
            this.createdBy = createdBy;
        }

        public Long getDeptId() {
            return deptId;
        }

        public void setDeptId(Long deptId) {
            this.deptId = deptId;
        }
    }

    @Test
    void applyAllScopeLeavesWrapperUntouched() {
        LambdaQueryWrapper<DummyEntity> wrapper = new LambdaQueryWrapper<>();
        DataScopeUtil.apply(wrapper, EffectiveDataScope.all(), 1L, DummyEntity::getCreatedBy, DummyEntity::getDeptId);
        String sql = wrapper.getSqlSegment();
        assertTrue(sql == null || sql.isBlank(), "All scope should not add filters, but was: " + sql);
        assertTrue(wrapper.getParamNameValuePairs().isEmpty());
    }

    @Test
    void applySelfOnlyAddsCreatorPredicate() {
        LambdaQueryWrapper<DummyEntity> wrapper = new LambdaQueryWrapper<>();
        EffectiveDataScope scope = EffectiveDataScope.selfOnly();
        DataScopeUtil.apply(wrapper, scope, 42L, DummyEntity::getCreatedBy, DummyEntity::getDeptId);
        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("created_by"));
        assertFalse(sql.contains("dept_id"));
        assertEquals(1, wrapper.getParamNameValuePairs().size());
        assertTrue(wrapper.getParamNameValuePairs().values().contains(42L));
    }

    @Test
    void applyDeptOnlyAddsInFilter() {
        LambdaQueryWrapper<DummyEntity> wrapper = new LambdaQueryWrapper<>();
        EffectiveDataScope scope = EffectiveDataScope.ofDepartments(Set.of(10L, 11L), false);
        DataScopeUtil.apply(wrapper, scope, 99L, DummyEntity::getCreatedBy, DummyEntity::getDeptId);
        String sql = wrapper.getSqlSegment();
        assertFalse(sql.contains("created_by"));
        assertTrue(sql.contains("dept_id"));
        assertFalse(wrapper.getParamNameValuePairs().isEmpty());
        assertTrue(wrapper.getParamNameValuePairs().values().stream()
                .filter(v -> v instanceof Iterable<?>)
                .map(Iterable.class::cast)
                .flatMap(iterable -> {
                    java.util.List<Object> list = new java.util.ArrayList<>();
                    iterable.forEach(list::add);
                    return list.stream();
                })
                .anyMatch(v -> v.equals(10L) || v.equals(11L)));
    }

    @Test
    void applySelfOrDeptWrapsWithOrCondition() {
        LambdaQueryWrapper<DummyEntity> wrapper = new LambdaQueryWrapper<>();
        EffectiveDataScope scope = EffectiveDataScope.ofDepartments(Set.of(5L), true);
        DataScopeUtil.apply(wrapper, scope, 123L, DummyEntity::getCreatedBy, DummyEntity::getDeptId);
        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("created_by"));
        assertTrue(sql.contains("dept_id"));
        assertTrue(sql.contains("OR"));
    }

    @Test
    void applyQueryWrapperVariantBuildsClause() {
        QueryWrapper<DummyEntity> wrapper = new QueryWrapper<>();
        EffectiveDataScope scope = EffectiveDataScope.ofDepartments(Collections.emptySet(), false);
        DataScopeUtil.apply(wrapper, scope, 9L, "created_by", "dept_id");
        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("1 = 0"));
        assertTrue(wrapper.getParamNameValuePairs().isEmpty());
    }
}
