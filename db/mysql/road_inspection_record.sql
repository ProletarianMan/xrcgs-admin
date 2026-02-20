-- -----------------------------------------------------
-- Table structure for road inspection record feature

CREATE DATABASE IF NOT EXISTS `xrcgs_roadsafety`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE `xrcgs_roadsafety`;

DROP TABLE IF EXISTS `road_inspection_photo`;
DROP TABLE IF EXISTS `road_inspection_handling_detail`;
DROP TABLE IF EXISTS `road_inspection_record`;

CREATE TABLE `road_inspection_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `record_date` DATE NOT NULL COMMENT '巡查日期',
  `unit_name` VARCHAR(100) NOT NULL COMMENT '单位名称',
  `weather` VARCHAR(50) DEFAULT NULL COMMENT '天气情况',
  `patrol_team` VARCHAR(100) DEFAULT NULL COMMENT '巡查人员或班组',
  `patrol_vehicle` VARCHAR(100) DEFAULT NULL COMMENT '巡查车辆',
  `location` VARCHAR(255) DEFAULT NULL COMMENT '巡查路线、里程与桩号',
  `inspection_content` TEXT COMMENT '巡查内容概述',
  `issues_found` TEXT COMMENT '发现的问题描述',
  `handling_situation_raw` TEXT COMMENT '原始处理情况文本',
  `handover_summary` TEXT COMMENT '车辆装备案件交接情况',
  `remark` TEXT COMMENT '备注',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  `exported_by` VARCHAR(64) DEFAULT NULL COMMENT '导出人',
  `exported_at` DATETIME(6) DEFAULT NULL COMMENT '导出时间',
  `export_file_name` VARCHAR(255) DEFAULT NULL COMMENT '导出文件名',
  `approval_status` VARCHAR(32) NOT NULL DEFAULT 'UNSUBMITTED' COMMENT '审批状态',
  `squad_code` VARCHAR(64) NOT NULL COMMENT '所属中队编码',
  PRIMARY KEY (`id`),
  KEY `idx_record_date` (`record_date`),
  KEY `idx_squad_code` (`squad_code`),
  KEY `idx_created_by` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='路产安全巡查记录';

CREATE TABLE `road_inspection_handling_detail` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `record_id` BIGINT UNSIGNED NOT NULL COMMENT '巡查记录ID',
  `category_code` VARCHAR(50) NOT NULL COMMENT '处理分类编码',
  `category_name` VARCHAR(100) NOT NULL COMMENT '处理分类名称',
  `detail_text` TEXT NOT NULL COMMENT '处理事项详情',
  `detail_order` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_record_category` (`record_id`, `category_code`),
  CONSTRAINT `fk_handling_record` FOREIGN KEY (`record_id`) REFERENCES `road_inspection_record` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='巡查记录处理分类明细';

CREATE TABLE `road_inspection_photo` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `record_id` BIGINT UNSIGNED NOT NULL COMMENT '巡查记录ID',
  `file_id` BIGINT UNSIGNED NOT NULL COMMENT '文件ID',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '图片说明',
  `sort_order` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_record_sort` (`record_id`, `sort_order`),
  KEY `idx_file_id` (`file_id`),
  CONSTRAINT `fk_photo_record` FOREIGN KEY (`record_id`) REFERENCES `road_inspection_record` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='巡查记录照片信息';
