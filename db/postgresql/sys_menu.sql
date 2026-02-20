-- PostgreSQL 16 初始化脚本：xrcgs_admin 菜单权限表
-- CREATE DATABASE xrcgs_admin;
-- COMMENT ON DATABASE xrcgs_admin IS 'XRCGS 管理后台主业务数据库';

CREATE TABLE IF NOT EXISTS sys_menu (
  id BIGINT PRIMARY KEY,
  parent_id BIGINT DEFAULT 0,
  title VARCHAR(64) NOT NULL,
  path VARCHAR(128),
  router_name VARCHAR(64),
  component VARCHAR(128),
  type VARCHAR(16) NOT NULL,
  perms VARCHAR(128) UNIQUE,
  icon VARCHAR(64),
  rank INTEGER DEFAULT 0,
  keep_alive SMALLINT DEFAULT 0,
  show_parent SMALLINT DEFAULT 0,
  visible SMALLINT DEFAULT 1,
  status SMALLINT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  del_flag SMALLINT DEFAULT 0
);

COMMENT ON TABLE sys_menu IS '菜单权限表';
COMMENT ON COLUMN sys_menu.id IS '主键 ID';
COMMENT ON COLUMN sys_menu.parent_id IS '父级菜单 ID，0 表示根节点';
COMMENT ON COLUMN sys_menu.title IS '菜单标题';
COMMENT ON COLUMN sys_menu.path IS '前端路由路径';
COMMENT ON COLUMN sys_menu.router_name IS '前端路由名称';
COMMENT ON COLUMN sys_menu.component IS '前端组件路径';
COMMENT ON COLUMN sys_menu.type IS '菜单类型：DIR/MENU/BUTTON/API';
COMMENT ON COLUMN sys_menu.perms IS '权限标识，例如 iam:user:list';
COMMENT ON COLUMN sys_menu.icon IS '菜单图标';
COMMENT ON COLUMN sys_menu.rank IS '排序权重，越小越靠前';
COMMENT ON COLUMN sys_menu.keep_alive IS '是否开启 keepAlive：0-否，1-是';
COMMENT ON COLUMN sys_menu.show_parent IS '子路由是否显示父级：0-否，1-是';
COMMENT ON COLUMN sys_menu.visible IS '是否可见：1-可见，0-隐藏';
COMMENT ON COLUMN sys_menu.status IS '状态：1-启用，0-禁用';
COMMENT ON COLUMN sys_menu.created_at IS '创建时间';
COMMENT ON COLUMN sys_menu.updated_at IS '更新时间';
COMMENT ON COLUMN sys_menu.del_flag IS '逻辑删除标记：0-正常，1-删除';
