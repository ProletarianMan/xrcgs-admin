-- PostgreSQL 16 初始化脚本：xrcgs_admin 部门表
-- CREATE DATABASE xrcgs_admin;
-- COMMENT ON DATABASE xrcgs_admin IS 'XRCGS 管理后台主业务数据库';

CREATE TABLE IF NOT EXISTS sys_dept (
  id BIGINT PRIMARY KEY,
  parent_id BIGINT NOT NULL DEFAULT 0,
  path VARCHAR(512) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(100),
  status SMALLINT NOT NULL DEFAULT 1,
  sort_no INTEGER NOT NULL DEFAULT 0,
  leader_user_id BIGINT,
  phone VARCHAR(30),
  email VARCHAR(100),
  remark VARCHAR(255),
  create_by BIGINT,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  del_flag SMALLINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_sys_dept_parent_name UNIQUE (parent_id, name),
  CONSTRAINT chk_sys_dept_path_format CHECK (path ~ '^(/[0-9]+)+/$')
);

CREATE INDEX IF NOT EXISTS idx_sys_dept_parent_id ON sys_dept(parent_id);
CREATE INDEX IF NOT EXISTS idx_sys_dept_sort_no ON sys_dept(parent_id, sort_no);

COMMENT ON TABLE sys_dept IS '系统部门表';
COMMENT ON COLUMN sys_dept.id IS '主键 ID';
COMMENT ON COLUMN sys_dept.parent_id IS '父部门 ID，0 表示顶级';
COMMENT ON COLUMN sys_dept.path IS '物化路径，例如 /1/3/5/';
COMMENT ON COLUMN sys_dept.name IS '部门名称';
COMMENT ON COLUMN sys_dept.code IS '部门编码';
COMMENT ON COLUMN sys_dept.status IS '状态：1-启用，0-禁用';
COMMENT ON COLUMN sys_dept.sort_no IS '排序号，越小越靠前';
COMMENT ON COLUMN sys_dept.leader_user_id IS '负责人用户 ID';
COMMENT ON COLUMN sys_dept.phone IS '联系电话';
COMMENT ON COLUMN sys_dept.email IS '联系邮箱';
COMMENT ON COLUMN sys_dept.remark IS '备注';
COMMENT ON COLUMN sys_dept.create_by IS '创建人 ID';
COMMENT ON COLUMN sys_dept.create_time IS '创建时间';
COMMENT ON COLUMN sys_dept.update_by IS '更新人 ID';
COMMENT ON COLUMN sys_dept.update_time IS '更新时间';
COMMENT ON COLUMN sys_dept.del_flag IS '逻辑删除标记：0-在用，1-删除';
