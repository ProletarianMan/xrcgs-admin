ALTER TABLE public.road_inspection_record
  ADD COLUMN IF NOT EXISTS form_payload_json text,
  ADD COLUMN IF NOT EXISTS details_payload_json text,
  ADD COLUMN IF NOT EXISTS summary_payload_json text;

