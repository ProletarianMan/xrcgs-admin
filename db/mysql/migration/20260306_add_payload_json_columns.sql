ALTER TABLE `road_inspection_record`
  ADD COLUMN IF NOT EXISTS `form_payload_json` LONGTEXT COMMENT '原始提交payload' AFTER `export_file_name`,
  ADD COLUMN IF NOT EXISTS `details_payload_json` LONGTEXT COMMENT 'details原始payload' AFTER `form_payload_json`,
  ADD COLUMN IF NOT EXISTS `summary_payload_json` LONGTEXT COMMENT 'summary标准化快照' AFTER `details_payload_json`;

