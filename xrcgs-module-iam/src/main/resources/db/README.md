# IAM 模块数据库脚本

本目录收纳 IAM 模块相关的结构初始化与示例脚本：

- `001_create_sys_dept.sql`：创建部门表 `sys_dept`，使用物化路径维护组织层级。
- `002_alter_sys_user_role_add_dept_scope.sql`：为 `sys_user`、`sys_role` 补齐部门/数据范围字段，并确保 JSON 字段类型正确。
- `003_seed_sys_dept_and_path_backfill.sql`：示例部门数据、部门迁移示例及通用 path 回填脚本。
- `004_create_or_alter_sys_user_contact.sql`：提供包含 `wechat_id`、`phone`、`gender` 字段的完整 `sys_user` 建表语句，并在增量场景下补齐联系方式及性别列。

## `path` 字段的物化路径语义

`sys_dept.path` 采用“物化路径 (materialized path)”方案表示上下级关系，格式约定如下：

1. **以 `/` 作为分隔符，并同时作为前缀与后缀**。例如根部门 ID=1 的 path 为 `/1/`，其子部门 ID=2 的 path 为 `/1/2/`。
2. **完整路径由父节点 path 加上当前部门 ID 拼接得到**：`child.path = CONCAT(parent.path, child.id, '/')`。
3. 依赖 path 可以快速完成如下查询：
   - `SELECT * FROM sys_dept WHERE path LIKE '/1/%'` 查询所有属于“集团总部”的部门。
   - `ORDER BY path` 即可按层级输出结构化列表。
4. `chk_sys_dept_path_format` 检查约束会校验 path 必须满足上述格式（至少包含一个 ID，且均为数字）。

> **两段式插入建议**：若通过自增主键插入，可先写入一个临时唯一 path（例如 `CONCAT('#pending#', UUID())`），获取自增 ID 后再执行 `UPDATE` 将 path 改写为父级 path + ID 的正式格式。参见 `003_seed_sys_dept_and_path_backfill.sql` 中的示例。

## 示例脚本说明

- **初始化组织架构**：执行 `003_seed_sys_dept_and_path_backfill.sql` 顶部的 INSERT 语句，可重复执行（`ON DUPLICATE KEY UPDATE` 保证幂等），用于快速造一份标准数据。
- **迁移部门**：脚本中提供了将“平台研发二部”改挂到“市场部”的 `UPDATE` 示范，展示修改父级时同步刷新 path 的方式。
- **回填 path**：当导入老数据或 path 失真时，执行同一脚本中的 `WITH RECURSIVE` 回填语句，可一次性重新计算所有路径；必要时对孤儿节点再执行最后的补丁语句。

## 本地验证建议

1. 在本地数据库（例如 MySQL 8.0）中新建 schema，并按顺序执行 `001`、`002`、`003` 三个脚本。
2. 使用 Navicat/Datagrip 等客户端检查：
   - `sys_dept` 包含 `parent_id`、`path`、`chk_sys_dept_path_format` 约束以及 `uk_sys_dept_path` 等索引；
   - `sys_user`、`sys_role` 出现新的 JSON 字段（`extra_dept_ids`、`data_scope_ext`）且 `dept_id` 索引可见。
3. 按需执行回填脚本，再查询 `SELECT id, parent_id, path FROM sys_dept ORDER BY path;`，确认路径已正确落地。
