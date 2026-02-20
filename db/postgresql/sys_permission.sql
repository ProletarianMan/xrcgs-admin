-- PostgreSQL 16 初始化脚本：xrcgs_admin 独立权限表
-- CREATE DATABASE xrcgs_admin;
-- COMMENT ON DATABASE xrcgs_admin IS 'XRCGS 管理后台主业务数据库';

CREATE TABLE IF NOT EXISTS sys_permission (
  id BIGINT PRIMARY KEY,
  code VARCHAR(128) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_permission IS '独立权限表';
COMMENT ON COLUMN sys_permission.id IS '主键 ID';
COMMENT ON COLUMN sys_permission.code IS '权限编码，例如 file:doc:convert';
COMMENT ON COLUMN sys_permission.name IS '权限名称';
COMMENT ON COLUMN sys_permission.created_at IS '创建时间';
COMMENT ON COLUMN sys_permission.updated_at IS '更新时间';
