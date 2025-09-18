-- -----------------------------------------------------------------------------
-- Alter: sys_user, sys_role
-- -----------------------------------------------------------------------------
-- 为用户和角色表补齐部门（dept_id、extra_dept_ids）及数据范围（data_scope、data_scope_ext）字段。
-- data_scope_ext / extra_dept_ids 使用 JSON 存储，方便直接解析为数组。
-- -----------------------------------------------------------------------------

-- sys_user --------------------------------------------------------------------
ALTER TABLE `sys_user`
    ADD COLUMN IF NOT EXISTS `dept_id` BIGINT UNSIGNED NULL COMMENT '主部门 ID' AFTER `nickname`,
    ADD COLUMN IF NOT EXISTS `extra_dept_ids` JSON NULL COMMENT '附加部门 ID 列表（JSON 数组，如 [1,2,3]）' AFTER `dept_id`,
    ADD COLUMN IF NOT EXISTS `data_scope` VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT '数据范围：ALL/DEPT/DEPT_AND_CHILD/SELF/CUSTOM' AFTER `extra_dept_ids`,
    ADD COLUMN IF NOT EXISTS `data_scope_ext` JSON NULL COMMENT '扩展数据范围，CUSTOM 时存 JSON 数组' AFTER `data_scope`;

ALTER TABLE `sys_user`
    ADD INDEX IF NOT EXISTS `idx_sys_user_dept_id` (`dept_id`);

-- 保障 JSON 类型（如历史库中为 VARCHAR 时自动改为 JSON）。
ALTER TABLE `sys_user`
    MODIFY COLUMN `extra_dept_ids` JSON NULL COMMENT '附加部门 ID 列表（JSON 数组，如 [1,2,3]）',
    MODIFY COLUMN `data_scope_ext` JSON NULL COMMENT '扩展数据范围，CUSTOM 时存 JSON 数组';

UPDATE `sys_user`
SET `data_scope` = 'SELF'
WHERE `data_scope` IS NULL;

-- sys_role --------------------------------------------------------------------
ALTER TABLE `sys_role`
    ADD COLUMN IF NOT EXISTS `dept_id` BIGINT UNSIGNED NULL COMMENT '归属部门 ID' AFTER `name`,
    ADD COLUMN IF NOT EXISTS `extra_dept_ids` JSON NULL COMMENT '附加部门 ID 列表（JSON 数组）' AFTER `dept_id`,
    ADD COLUMN IF NOT EXISTS `data_scope` VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT '数据范围：ALL/DEPT/DEPT_AND_CHILD/SELF/CUSTOM' AFTER `extra_dept_ids`,
    ADD COLUMN IF NOT EXISTS `data_scope_ext` JSON NULL COMMENT '扩展数据范围，CUSTOM 时存 JSON 数组' AFTER `data_scope`;

ALTER TABLE `sys_role`
    ADD INDEX IF NOT EXISTS `idx_sys_role_dept_id` (`dept_id`);

ALTER TABLE `sys_role`
    MODIFY COLUMN `extra_dept_ids` JSON NULL COMMENT '附加部门 ID 列表（JSON 数组）',
    MODIFY COLUMN `data_scope_ext` JSON NULL COMMENT '扩展数据范围，CUSTOM 时存 JSON 数组';

UPDATE `sys_role`
SET `data_scope` = 'SELF'
WHERE `data_scope` IS NULL;
