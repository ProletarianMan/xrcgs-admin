CREATE TABLE `sys_role_perm` (
  `id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `perm_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `fk_rp_perm` (`perm_id`),
  KEY `fk_r_role` (`role_id`),
  CONSTRAINT `fk_r_perm` FOREIGN KEY (`perm_id`) REFERENCES `sys_permission` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_r_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-权限关联表';
