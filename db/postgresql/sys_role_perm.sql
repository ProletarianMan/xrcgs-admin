-- PostgreSQL 16 初始化脚本：xrcgs_admin 角色-权限关联表
-- CREATE DATABASE xrcgs_admin;
-- COMMENT ON DATABASE xrcgs_admin IS 'XRCGS 管理后台主业务数据库';

CREATE TABLE IF NOT EXISTS sys_role_perm (
  id BIGINT PRIMARY KEY,
  role_id BIGINT NOT NULL,
  perm_id BIGINT NOT NULL,
  CONSTRAINT fk_r_perm FOREIGN KEY (perm_id) REFERENCES sys_permission(id) ON DELETE RESTRICT,
  CONSTRAINT fk_r_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_rp_perm ON sys_role_perm(perm_id);
CREATE INDEX IF NOT EXISTS idx_rp_role ON sys_role_perm(role_id);

COMMENT ON TABLE sys_role_perm IS '角色-权限关联表';
COMMENT ON COLUMN sys_role_perm.id IS '主键 ID';
COMMENT ON COLUMN sys_role_perm.role_id IS '角色 ID';
COMMENT ON COLUMN sys_role_perm.perm_id IS '权限 ID';
