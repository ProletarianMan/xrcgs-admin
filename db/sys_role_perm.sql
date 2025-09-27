CREATE TABLE `sys_role_perm` (
  `id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `perm_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`,`perm_id`,`id`) USING BTREE,
  KEY `fk_rp_perm` (`perm_id`),
  CONSTRAINT `fk_rp_perm` FOREIGN KEY (`perm_id`) REFERENCES `sys_permission` (`id`),
  CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-权限关联表';
