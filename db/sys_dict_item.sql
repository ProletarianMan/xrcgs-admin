CREATE TABLE `sys_dict_item` (
  `id` bigint NOT NULL,
  `type_code` varchar(64) NOT NULL COMMENT '所属字典类型编码',
  `label` varchar(64) NOT NULL COMMENT '显示标签',
  `value` varchar(64) NOT NULL COMMENT '值',
  `sort` int DEFAULT '0',
  `ext` varchar(255) DEFAULT NULL COMMENT '扩展字段',
  `status` tinyint DEFAULT '1',
  `dept_id` bigint DEFAULT NULL COMMENT '归属部门 ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_di_type` (`type_code`),
  KEY `idx_sys_dict_item_dept_id` (`dept_id`),
  CONSTRAINT `fk_di_type` FOREIGN KEY (`type_code`) REFERENCES `sys_dict_type` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典项表';
