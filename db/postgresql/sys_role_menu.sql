-- PostgreSQL 16 初始化脚本：xrcgs_admin 角色-菜单关联表
-- CREATE DATABASE xrcgs_admin;
-- COMMENT ON DATABASE xrcgs_admin IS 'XRCGS 管理后台主业务数据库';

CREATE TABLE IF NOT EXISTS sys_role_menu (
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, menu_id),
  CONSTRAINT fk_rm_menu FOREIGN KEY (menu_id) REFERENCES sys_menu(id),
  CONSTRAINT fk_rm_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
);

CREATE INDEX IF NOT EXISTS idx_rm_menu ON sys_role_menu(menu_id);

COMMENT ON TABLE sys_role_menu IS '角色-菜单关联表';
COMMENT ON COLUMN sys_role_menu.role_id IS '角色 ID';
COMMENT ON COLUMN sys_role_menu.menu_id IS '菜单 ID';
