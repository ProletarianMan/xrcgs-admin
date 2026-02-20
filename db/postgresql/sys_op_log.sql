-- PostgreSQL 16 初始化脚本：xrcgs_admin 系统操作日志表
-- CREATE DATABASE xrcgs_admin;
-- COMMENT ON DATABASE xrcgs_admin IS 'XRCGS 管理后台主业务数据库';

CREATE TABLE IF NOT EXISTS sys_op_log (
  id BIGINT PRIMARY KEY,
  title VARCHAR(255),
  username VARCHAR(64),
  "methodSign" VARCHAR(255),
  "httpMethod" VARCHAR(16),
  uri VARCHAR(255),
  ip VARCHAR(64),
  "queryString" VARCHAR(1024),
  "reqBody" TEXT,
  "respBody" TEXT,
  success SMALLINT DEFAULT 1,
  "elapsedMs" BIGINT,
  "exceptionMsg" VARCHAR(1024),
  "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sysoplog_created ON sys_op_log("createdAt");
CREATE INDEX IF NOT EXISTS idx_sysoplog_user ON sys_op_log(username);
CREATE INDEX IF NOT EXISTS idx_sysoplog_uri ON sys_op_log(uri);
CREATE INDEX IF NOT EXISTS idx_sysoplog_title ON sys_op_log(title);

COMMENT ON TABLE sys_op_log IS '系统操作日志表';
COMMENT ON COLUMN sys_op_log.id IS '主键 ID';
COMMENT ON COLUMN sys_op_log.title IS '操作标题';
COMMENT ON COLUMN sys_op_log.username IS '操作人用户名';
COMMENT ON COLUMN sys_op_log."methodSign" IS '方法签名（类名#方法）';
COMMENT ON COLUMN sys_op_log."httpMethod" IS 'HTTP 方法';
COMMENT ON COLUMN sys_op_log.uri IS '请求 URI';
COMMENT ON COLUMN sys_op_log.ip IS '客户端 IP';
COMMENT ON COLUMN sys_op_log."queryString" IS 'URL 查询参数';
COMMENT ON COLUMN sys_op_log."reqBody" IS '请求体（截断存储）';
COMMENT ON COLUMN sys_op_log."respBody" IS '响应体（截断存储）';
COMMENT ON COLUMN sys_op_log.success IS '是否成功：1-成功，0-失败';
COMMENT ON COLUMN sys_op_log."elapsedMs" IS '耗时（毫秒）';
COMMENT ON COLUMN sys_op_log."exceptionMsg" IS '异常信息摘要';
COMMENT ON COLUMN sys_op_log."createdAt" IS '创建时间';
