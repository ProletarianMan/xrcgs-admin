-- PostgreSQL 16 初始化脚本：xrcgs_admin 用户-角色关联表
-- CREATE DATABASE xrcgs_admin;
-- COMMENT ON DATABASE xrcgs_admin IS 'XRCGS 管理后台主业务数据库';

CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_ur_role ON sys_user_role(role_id);
CREATE INDEX IF NOT EXISTS idx_ur_user ON sys_user_role(user_id);

COMMENT ON TABLE sys_user_role IS '用户-角色关联表';
COMMENT ON COLUMN sys_user_role.id IS '主键 ID';
COMMENT ON COLUMN sys_user_role.user_id IS '用户 ID';
COMMENT ON COLUMN sys_user_role.role_id IS '角色 ID';
