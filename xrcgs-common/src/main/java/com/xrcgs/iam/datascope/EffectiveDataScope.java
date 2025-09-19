package com.xrcgs.iam.datascope;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 描述用户有效数据范围的结果对象
 * combining role/user level rules.
 */
public class EffectiveDataScope {

    private boolean all;
    private boolean self;
    private Set<Long> deptIds = new LinkedHashSet<>();
    private Long deptTreeVersion;

    public EffectiveDataScope() {
    }

    private EffectiveDataScope(boolean all, boolean self, Collection<Long> deptIds) {
        this.all = all;
        this.self = self;
        setDeptIds(deptIds);
    }

    public static EffectiveDataScope all() {
        EffectiveDataScope scope = new EffectiveDataScope(true, true, Collections.emptySet());
        scope.deptTreeVersion = 0L;
        return scope;
    }

    public static EffectiveDataScope selfOnly() {
        EffectiveDataScope scope = new EffectiveDataScope(false, true, Collections.emptySet());
        scope.deptTreeVersion = 0L;
        return scope;
    }

    public static EffectiveDataScope ofDepartments(Collection<Long> deptIds, boolean includeSelf) {
        return new EffectiveDataScope(false, includeSelf, deptIds);
    }

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    public boolean isSelf() {
        return self;
    }

    public void setSelf(boolean self) {
        this.self = self;
    }

    public Set<Long> getDeptIds() {
        return Collections.unmodifiableSet(deptIds);
    }

    public void setDeptIds(Collection<Long> deptIds) {
        this.deptIds.clear();
        if (deptIds == null) {
            return;
        }
        for (Long id : deptIds) {
            if (id != null) {
                this.deptIds.add(id);
            }
        }
    }

    @JsonIgnore
    public boolean hasDepartments() {
        return !deptIds.isEmpty();
    }

    public Long getDeptTreeVersion() {
        return deptTreeVersion;
    }

    public void setDeptTreeVersion(Long deptTreeVersion) {
        this.deptTreeVersion = deptTreeVersion;
    }

    public EffectiveDataScope copy() {
        EffectiveDataScope scope = new EffectiveDataScope(all, self, deptIds);
        scope.setDeptTreeVersion(deptTreeVersion);
        return scope;
    }

    @Override
    public String toString() {
        return "EffectiveDataScope{" +
                "all=" + all +
                ", self=" + self +
                ", deptIds=" + deptIds +
                ", deptTreeVersion=" + deptTreeVersion +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EffectiveDataScope that)) return false;
        return all == that.all && self == that.self && Objects.equals(deptIds, that.deptIds)
                && Objects.equals(deptTreeVersion, that.deptTreeVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(all, self, deptIds, deptTreeVersion);
    }
}
