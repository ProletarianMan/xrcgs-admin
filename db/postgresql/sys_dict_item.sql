-- PostgreSQL 16 初始化脚本：xrcgs_admin 字典项表
-- CREATE DATABASE xrcgs_admin;
-- COMMENT ON DATABASE xrcgs_admin IS 'XRCGS 管理后台主业务数据库';

CREATE TABLE IF NOT EXISTS sys_dict_item (
  id BIGINT PRIMARY KEY,
  type_code VARCHAR(64) NOT NULL,
  label VARCHAR(64) NOT NULL,
  value VARCHAR(64) NOT NULL,
  sort INTEGER DEFAULT 0,
  ext VARCHAR(255),
  status SMALLINT DEFAULT 1,
  dept_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_di_type FOREIGN KEY (type_code) REFERENCES sys_dict_type(code)
);

CREATE INDEX IF NOT EXISTS idx_sys_dict_item_dept_id ON sys_dict_item(dept_id);

COMMENT ON TABLE sys_dict_item IS '字典项表';
COMMENT ON COLUMN sys_dict_item.id IS '主键 ID';
COMMENT ON COLUMN sys_dict_item.type_code IS '所属字典类型编码';
COMMENT ON COLUMN sys_dict_item.label IS '字典项显示标签';
COMMENT ON COLUMN sys_dict_item.value IS '字典项值';
COMMENT ON COLUMN sys_dict_item.sort IS '排序值，越小越靠前';
COMMENT ON COLUMN sys_dict_item.ext IS '扩展字段';
COMMENT ON COLUMN sys_dict_item.status IS '启用状态：1-启用，0-禁用';
COMMENT ON COLUMN sys_dict_item.dept_id IS '归属部门 ID';
COMMENT ON COLUMN sys_dict_item.created_at IS '创建时间';
COMMENT ON COLUMN sys_dict_item.updated_at IS '更新时间';
