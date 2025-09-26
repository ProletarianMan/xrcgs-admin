-- -----------------------------------------------------------------------------
-- Create or alter: sys_user contact columns
-- -----------------------------------------------------------------------------
-- 提供包含 wechat_id、phone 字段的完整 sys_user 建表语句，
-- 并在增量场景下补齐缺失的联系方式列。
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username` VARCHAR(64) NOT NULL COMMENT '登录用户名',
    `password` VARCHAR(128) NOT NULL COMMENT '密码（BCrypt）',
    `nickname` VARCHAR(64) NOT NULL COMMENT '昵称',
    `wechat_id` VARCHAR(64) NULL DEFAULT NULL COMMENT '微信号',
    `phone` VARCHAR(32) NULL DEFAULT NULL COMMENT '联系电话',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `dept_id` BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '主部门 ID',
    `extra_dept_ids` JSON NULL COMMENT '附加部门 ID 列表（JSON 数组）',
    `data_scope` VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT '数据范围',
    `data_scope_ext` JSON NULL COMMENT '扩展数据范围（CUSTOM 时有效）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username` (`username`),
    KEY `idx_sys_user_dept_id` (`dept_id`)
) COMMENT='系统用户';

ALTER TABLE `sys_user`
    ADD COLUMN IF NOT EXISTS `wechat_id` VARCHAR(64) NULL DEFAULT NULL COMMENT '微信号' AFTER `nickname`,
    ADD COLUMN IF NOT EXISTS `phone` VARCHAR(32) NULL DEFAULT NULL COMMENT '联系电话' AFTER `wechat_id`;
