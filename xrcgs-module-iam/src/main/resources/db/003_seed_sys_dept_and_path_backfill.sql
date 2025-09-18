-- -----------------------------------------------------------------------------
-- Seed data & helper scripts for sys_dept
-- -----------------------------------------------------------------------------
-- 1. 示例部门初始化（可重复执行，使用 ON DUPLICATE KEY UPDATE 保持幂等）。
-- 2. 部门改动示例（调整父子关系时同步维护 path）。
-- 3. 通用 path 回填脚本（当 path 缺失或批量导入后需要重算时使用）。
-- -----------------------------------------------------------------------------

SET @now := NOW();

INSERT INTO `sys_dept` (`id`, `parent_id`, `path`, `name`, `code`, `status`, `sort_no`, `create_time`, `update_time`)
VALUES
    (1, 0, '/1/',  '集团总部',      'HQ',        1, 10, @now, @now),
    (2, 1, '/1/2/', '研发中心',      'RND',       1, 20, @now, @now),
    (3, 1, '/1/3/', '市场部',        'MKT',       1, 30, @now, @now),
    (4, 2, '/1/2/4/', '平台研发一部', 'RND-PLAT',  1, 40, @now, @now),
    (5, 2, '/1/2/5/', '平台研发二部', 'RND-MOB',   1, 50, @now, @now)
ON DUPLICATE KEY UPDATE
    `parent_id`  = VALUES(`parent_id`),
    `path`       = VALUES(`path`),
    `name`       = VALUES(`name`),
    `code`       = VALUES(`code`),
    `status`     = VALUES(`status`),
    `sort_no`    = VALUES(`sort_no`),
    `update_time`= @now;

-- 调整部门父级示例：把「平台研发二部」移动到「市场部」下面。
-- 注意：path 必须同步更新为 parent.path + 当前 id + '/'.
UPDATE `sys_dept` AS child
JOIN `sys_dept` AS new_parent ON new_parent.id = 3
SET
    child.parent_id = new_parent.id,
    child.path      = CONCAT(new_parent.path, child.id, '/'),
    child.update_time = NOW(),
    child.update_by   = 1
WHERE child.id = 5;

-- -----------------------------------------------------------------------------
-- 通用 path 回填脚本
--  - 场景：1）老数据 path 缺失/格式不正确；2）使用脚本导入大量部门后统一刷新。
--  - 逻辑：从顶级部门（parent_id=0 或 parent_id IS NULL）开始向下递归，拼接父级 path。
-- -----------------------------------------------------------------------------
WITH RECURSIVE dept_tree AS (
    SELECT
        d.id,
        d.parent_id,
        CAST(CONCAT('/', d.id, '/') AS CHAR(512)) AS new_path
    FROM `sys_dept` AS d
    WHERE d.parent_id = 0 OR d.parent_id IS NULL

    UNION ALL

    SELECT
        child.id,
        child.parent_id,
        CAST(CONCAT(parent.new_path, child.id, '/') AS CHAR(512)) AS new_path
    FROM `sys_dept` AS child
    JOIN dept_tree AS parent ON child.parent_id = parent.id
)
UPDATE `sys_dept` AS d
JOIN dept_tree AS t ON d.id = t.id
SET d.path = t.new_path,
    d.update_time = NOW();

-- 对于层级存在孤儿节点（父级不存在）的情况，可单独处理：
-- UPDATE `sys_dept` SET `path` = CONCAT('/', `id`, '/') WHERE `parent_id` NOT IN (SELECT `id` FROM `sys_dept`);
