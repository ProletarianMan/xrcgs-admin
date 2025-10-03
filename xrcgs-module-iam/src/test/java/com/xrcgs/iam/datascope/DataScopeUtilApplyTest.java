package com.xrcgs.iam.datascope;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataScopeUtilApplyTest {

    @TableName("dummy_entity")
    private static final class DummyEntity {
        @TableField("created_by")
        private Long createdBy;
        @TableField("dept_id")
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
        ensureTableInfo();
        LambdaQueryWrapper<DummyEntity> wrapper = Wrappers.lambdaQuery(DummyEntity.class);
        DataScopeUtil.apply(wrapper, EffectiveDataScope.all(), 1L, DummyEntity::getCreatedBy, DummyEntity::getDeptId);
        String sql = wrapper.getSqlSegment();
        assertTrue(sql == null || sql.isBlank(), "All scope should not add filters, but was: " + sql);
        assertTrue(wrapper.getParamNameValuePairs().isEmpty());
    }

    @Test
    void applySelfOnlyAddsCreatorPredicate() {
        ensureTableInfo();
        LambdaQueryWrapper<DummyEntity> wrapper = Wrappers.lambdaQuery(DummyEntity.class);
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
        ensureTableInfo();
        LambdaQueryWrapper<DummyEntity> wrapper = Wrappers.lambdaQuery(DummyEntity.class);
        EffectiveDataScope scope = EffectiveDataScope.ofDepartments(Set.of(10L, 11L), false);
        DataScopeUtil.apply(wrapper, scope, 99L, DummyEntity::getCreatedBy, DummyEntity::getDeptId);
        String sql = wrapper.getSqlSegment();
        assertFalse(sql.contains("created_by"));
        assertTrue(sql.contains("dept_id"));
        assertFalse(wrapper.getParamNameValuePairs().isEmpty());
        String params = wrapper.getParamNameValuePairs().values().toString();
        assertTrue(params.contains("10") && params.contains("11"));
    }

    @Test
    void applySelfOrDeptWrapsWithOrCondition() {
        ensureTableInfo();
        LambdaQueryWrapper<DummyEntity> wrapper = Wrappers.lambdaQuery(DummyEntity.class);
        EffectiveDataScope scope = EffectiveDataScope.ofDepartments(Set.of(5L), true);
        DataScopeUtil.apply(wrapper, scope, 123L, DummyEntity::getCreatedBy, DummyEntity::getDeptId);
        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("created_by"));
        assertTrue(sql.contains("dept_id"));
        assertTrue(sql.contains("OR"));
    }

    @Test
    void applyQueryWrapperVariantBuildsClause() {
        ensureTableInfo();
        QueryWrapper<DummyEntity> wrapper = new QueryWrapper<>();
        EffectiveDataScope scope = EffectiveDataScope.ofDepartments(Collections.emptySet(), false);
        DataScopeUtil.apply(wrapper, scope, 9L, "created_by", "dept_id");
        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("1 = 0"));
        assertTrue(wrapper.getParamNameValuePairs().isEmpty());
    }

    private static void ensureTableInfo() {
        if (TableInfoHelper.getTableInfo(DummyEntity.class) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            TableInfoHelper.initTableInfo(assistant, DummyEntity.class);
        }
    }
}
