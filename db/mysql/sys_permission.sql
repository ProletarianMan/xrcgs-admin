CREATE TABLE `sys_permission` (
  `id` bigint NOT NULL,
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父权限ID，顶层为0',
  `code` varchar(128) NOT NULL COMMENT '权限码，例如 file:doc:convert',
  `name` varchar(64) NOT NULL COMMENT '权限名称',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='独立权限表';
