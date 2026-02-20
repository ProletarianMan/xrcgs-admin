-- PostgreSQL 16 初始化脚本：xrcgs_admin 用户表
-- CREATE DATABASE xrcgs_admin;
-- COMMENT ON DATABASE xrcgs_admin IS 'XRCGS 管理后台主业务数据库';

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(200) NOT NULL,
  nickname VARCHAR(64),
  wechat_id VARCHAR(64),
  phone VARCHAR(32),
  gender SMALLINT,
  dept_id BIGINT,
  extra_dept_ids JSONB,
  data_scope VARCHAR(32) NOT NULL DEFAULT 'SELF',
  data_scope_ext JSONB,
  enabled SMALLINT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_user_dept_id ON sys_user(dept_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_nickname ON sys_user(nickname);

COMMENT ON TABLE sys_user IS '系统用户表';
COMMENT ON COLUMN sys_user.id IS '主键 ID';
COMMENT ON COLUMN sys_user.username IS '登录用户名';
COMMENT ON COLUMN sys_user.password IS '登录密码（密文）';
COMMENT ON COLUMN sys_user.nickname IS '用户昵称';
COMMENT ON COLUMN sys_user.wechat_id IS '微信号';
COMMENT ON COLUMN sys_user.phone IS '联系电话';
COMMENT ON COLUMN sys_user.gender IS '性别：0-女，1-男';
COMMENT ON COLUMN sys_user.dept_id IS '主部门 ID';
COMMENT ON COLUMN sys_user.extra_dept_ids IS '附加部门 ID 列表（JSON 数组）';
COMMENT ON COLUMN sys_user.data_scope IS '数据范围：ALL/DEPT/DEPT_AND_CHILD/SELF/CUSTOM';
COMMENT ON COLUMN sys_user.data_scope_ext IS '扩展数据范围（CUSTOM 时为 JSON 数组）';
COMMENT ON COLUMN sys_user.enabled IS '是否启用：1-启用，0-禁用';
COMMENT ON COLUMN sys_user.created_at IS '创建时间';
COMMENT ON COLUMN sys_user.updated_at IS '更新时间';
