-- PostgreSQL 16 初始化脚本：xrcgs_admin 角色表
-- CREATE DATABASE xrcgs_admin;
-- COMMENT ON DATABASE xrcgs_admin IS 'XRCGS 管理后台主业务数据库';

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT PRIMARY KEY,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  dept_id BIGINT,
  extra_dept_ids JSONB,
  status SMALLINT NOT NULL DEFAULT 1,
  data_scope VARCHAR(32) NOT NULL DEFAULT 'ALL',
  data_scope_ext JSONB,
  remark VARCHAR(255),
  sort_no INTEGER NOT NULL DEFAULT 0,
  create_by BIGINT,
  create_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT,
  update_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  del_flag SMALLINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_role_datascope CHECK (data_scope IN ('ALL','DEPT','DEPT_AND_CHILD','SELF','CUSTOM')),
  CONSTRAINT chk_role_status CHECK (status IN (0,1))
);

CREATE INDEX IF NOT EXISTS idx_role_status ON sys_role(status);
CREATE INDEX IF NOT EXISTS idx_role_sort ON sys_role(sort_no);
CREATE INDEX IF NOT EXISTS idx_sys_role_dept_id ON sys_role(dept_id);

COMMENT ON TABLE sys_role IS '角色表';
COMMENT ON COLUMN sys_role.id IS '主键 ID';
COMMENT ON COLUMN sys_role.code IS '角色编码，例如 ADMIN/OPS/USER';
COMMENT ON COLUMN sys_role.name IS '角色名称';
COMMENT ON COLUMN sys_role.dept_id IS '归属部门 ID';
COMMENT ON COLUMN sys_role.extra_dept_ids IS '附加部门 ID 列表（JSON 数组）';
COMMENT ON COLUMN sys_role.status IS '状态：1-启用，0-禁用';
COMMENT ON COLUMN sys_role.data_scope IS '数据范围：ALL/DEPT/DEPT_AND_CHILD/SELF/CUSTOM';
COMMENT ON COLUMN sys_role.data_scope_ext IS '扩展数据范围（CUSTOM 时为 JSON 数组）';
COMMENT ON COLUMN sys_role.remark IS '备注';
COMMENT ON COLUMN sys_role.sort_no IS '排序号，越小越靠前';
COMMENT ON COLUMN sys_role.create_by IS '创建人 ID';
COMMENT ON COLUMN sys_role.create_time IS '创建时间';
COMMENT ON COLUMN sys_role.update_by IS '更新人 ID';
COMMENT ON COLUMN sys_role.update_time IS '更新时间';
COMMENT ON COLUMN sys_role.del_flag IS '逻辑删除标记：0-正常，1-删除';
