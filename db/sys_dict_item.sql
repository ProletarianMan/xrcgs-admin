CREATE TABLE `sys_dict_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type_code` varchar(64) NOT NULL COMMENT '所属字典类型编码',
  `label` varchar(64) NOT NULL COMMENT '显示标签',
  `value` varchar(64) NOT NULL COMMENT '值',
  `sort` int DEFAULT '0',
  `ext` varchar(255) DEFAULT NULL COMMENT '扩展字段',
  `status` tinyint DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_di_type` (`type_code`),
  CONSTRAINT `fk_di_type` FOREIGN KEY (`type_code`) REFERENCES `sys_dict_type` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典项表';
