-- Keep only the latest row for the same (record_date, squad_code)
-- Ordering rule: newer updated_at wins; if equal, larger id wins.
DELETE FROM public.road_inspection_record older
USING public.road_inspection_record newer
WHERE older.record_date = newer.record_date
  AND older.squad_code = newer.squad_code
  AND (
    older.updated_at < newer.updated_at
    OR (older.updated_at = newer.updated_at AND older.id < newer.id)
  );

CREATE UNIQUE INDEX IF NOT EXISTS uk_record_date_squad_code
  ON public.road_inspection_record (record_date, squad_code);
