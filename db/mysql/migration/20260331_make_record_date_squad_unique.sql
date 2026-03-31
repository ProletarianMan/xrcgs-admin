-- Keep only the latest row for the same (record_date, squad_code)
-- Ordering rule: newer updated_at wins; if equal, larger id wins.
DELETE older
FROM road_inspection_record older
JOIN road_inspection_record newer
  ON older.record_date = newer.record_date
 AND older.squad_code = newer.squad_code
 AND (
      older.updated_at < newer.updated_at
   OR (older.updated_at = newer.updated_at AND older.id < newer.id)
 );

-- Add unique index once deduplication is done.
SET @has_uk_record_date_squad_code := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'road_inspection_record'
    AND index_name = 'uk_record_date_squad_code'
);

SET @create_uk_sql := IF(
  @has_uk_record_date_squad_code = 0,
  'ALTER TABLE `road_inspection_record` ADD UNIQUE KEY `uk_record_date_squad_code` (`record_date`, `squad_code`)',
  'SELECT 1'
);

PREPARE stmt_create_uk FROM @create_uk_sql;
EXECUTE stmt_create_uk;
DEALLOCATE PREPARE stmt_create_uk;
