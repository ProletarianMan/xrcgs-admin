-- -----------------------------------------------------------------------------
-- Schema: sys_dept
-- -----------------------------------------------------------------------------
-- 部门（组织架构）表，采用物化路径（materialized path）存储整棵树的上下级关系。
-- path 字段以「/」分隔 ID，例如「/1/」「/1/3/」「/1/3/8/」。
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dept` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `parent_id`       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父部门 ID，0 表示顶级',
    `path`            VARCHAR(512)    NOT NULL COMMENT '物化路径 /1/3/5/，用于层级查询',
    `name`            VARCHAR(100)    NOT NULL COMMENT '部门名称',
    `code`            VARCHAR(100)             DEFAULT NULL COMMENT '部门编码，选填',
    `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1=启用，0=禁用',
    `sort_no`         INT             NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
    `leader_user_id`  BIGINT UNSIGNED          DEFAULT NULL COMMENT '负责人用户 ID',
    `phone`           VARCHAR(30)              DEFAULT NULL COMMENT '联系电话',
    `email`           VARCHAR(100)             DEFAULT NULL COMMENT '联系邮箱',
    `remark`          VARCHAR(255)             DEFAULT NULL COMMENT '备注',
    `create_by`       BIGINT UNSIGNED          DEFAULT NULL COMMENT '创建人',
    `create_time`     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       BIGINT UNSIGNED          DEFAULT NULL COMMENT '更新人',
    `update_time`     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`        TINYINT          NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=在用，1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_dept_path` (`path`),
    UNIQUE KEY `uk_sys_dept_parent_name` (`parent_id`, `name`),
    KEY `idx_sys_dept_parent_id` (`parent_id`),
    KEY `idx_sys_dept_sort_no` (`parent_id`, `sort_no`),
    CONSTRAINT `chk_sys_dept_path_format` CHECK (`path` REGEXP '^(/[0-9]+)+/$')
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT '系统部门';
