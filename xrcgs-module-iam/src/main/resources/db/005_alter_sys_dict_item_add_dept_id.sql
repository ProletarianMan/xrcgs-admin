-- 为字典项表增加部门范围控制字段
ALTER TABLE `sys_dict_item`
    ADD COLUMN IF NOT EXISTS `dept_id` BIGINT NULL COMMENT '归属部门 ID' AFTER `status`,
    ADD INDEX IF NOT EXISTS `idx_sys_dict_item_dept_id` (`dept_id`);
