-- PostgreSQL 16 初始化脚本：xrcgs_admin 文件表
-- CREATE DATABASE xrcgs_admin;
-- COMMENT ON DATABASE xrcgs_admin IS 'XRCGS 管理后台主业务数据库';

CREATE TABLE IF NOT EXISTS sys_file (
  id BIGINT PRIMARY KEY,
  biz_type VARCHAR(64) NOT NULL,
  file_type VARCHAR(16) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  ext VARCHAR(32) NOT NULL,
  mime VARCHAR(128) NOT NULL,
  size BIGINT NOT NULL,
  sha256 CHAR(64) NOT NULL UNIQUE,
  storage_path VARCHAR(512) NOT NULL,
  preview_path VARCHAR(512),
  status VARCHAR(16) NOT NULL DEFAULT 'UPLOADED',
  error_msg VARCHAR(1024),
  dept_id BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_file_biz_type ON sys_file(biz_type);
CREATE INDEX IF NOT EXISTS idx_file_type ON sys_file(file_type);
CREATE INDEX IF NOT EXISTS idx_file_created_at ON sys_file(created_at);
CREATE INDEX IF NOT EXISTS idx_file_dept_id ON sys_file(dept_id);

COMMENT ON TABLE sys_file IS '系统文件表';
COMMENT ON COLUMN sys_file.id IS '主键 ID';
COMMENT ON COLUMN sys_file.biz_type IS '业务类型';
COMMENT ON COLUMN sys_file.file_type IS '文件大类：IMAGE/DOC/VIDEO/AUDIO';
COMMENT ON COLUMN sys_file.original_name IS '原始文件名';
COMMENT ON COLUMN sys_file.ext IS '文件扩展名（不含点）';
COMMENT ON COLUMN sys_file.mime IS 'MIME 类型';
COMMENT ON COLUMN sys_file.size IS '文件大小（字节）';
COMMENT ON COLUMN sys_file.sha256 IS '文件 SHA-256 哈希';
COMMENT ON COLUMN sys_file.storage_path IS '物理存储相对路径';
COMMENT ON COLUMN sys_file.preview_path IS '预览/转换输出相对路径';
COMMENT ON COLUMN sys_file.status IS '文件状态：UPLOADED/CONVERTING/READY/FAIL/DELETED';
COMMENT ON COLUMN sys_file.error_msg IS '失败错误信息';
COMMENT ON COLUMN sys_file.dept_id IS '归属部门 ID';
COMMENT ON COLUMN sys_file.created_at IS '创建时间';
COMMENT ON COLUMN sys_file.created_by IS '创建人用户 ID';
