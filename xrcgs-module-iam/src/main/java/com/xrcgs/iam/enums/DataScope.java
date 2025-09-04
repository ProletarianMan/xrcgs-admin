package com.xrcgs.iam.enums;

/**
 * 角色状态，用在 sys_role 表的 data_scope 字段，决定该角色能看到哪些数据。
 * ALL
 * 👉 全部数据，无任何限制。一般给 超级管理员 用。
 * DEPT
 * 👉 仅限本部门的数据。例如用户属于「养护部」，则只能查询养护部的业务数据。
 * DEPT_AND_CHILD
 * 👉 本部门 + 下属部门的数据。例如「养护部」能看到自己和「养护部下属的施工小组」的数据。
 * SELF
 * 👉 只能看自己创建的数据（create_by = 当前用户）。常用于普通员工账号。
 * CUSTOM
 * 👉 自定义部门范围。具体 ID 数组存到 data_scope_ext（JSON），比如 "[1,2,3]" 表示能看部门 1、2、3 的数据。
 *
 */
public enum DataScope {
    ALL, DEPT, DEPT_AND_CHILD, SELF, CUSTOM
}
