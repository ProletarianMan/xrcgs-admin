-- PostgreSQL 16 初始化脚本：xrcgs_admin 字典类型表
-- CREATE DATABASE xrcgs_admin;
-- COMMENT ON DATABASE xrcgs_admin IS 'XRCGS 管理后台主业务数据库';

CREATE TABLE IF NOT EXISTS sys_dict_type (
  id BIGINT PRIMARY KEY,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  status SMALLINT DEFAULT 1,
  remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_dict_type IS '字典类型表';
COMMENT ON COLUMN sys_dict_type.id IS '主键 ID';
COMMENT ON COLUMN sys_dict_type.code IS '字典类型编码';
COMMENT ON COLUMN sys_dict_type.name IS '字典类型名称';
COMMENT ON COLUMN sys_dict_type.status IS '启用状态：1-启用，0-禁用';
COMMENT ON COLUMN sys_dict_type.remark IS '备注';
COMMENT ON COLUMN sys_dict_type.created_at IS '创建时间';
COMMENT ON COLUMN sys_dict_type.updated_at IS '更新时间';
