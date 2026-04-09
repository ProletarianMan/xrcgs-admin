/*
 Navicat Premium Dump SQL

 Source Server         : t14p pg16
 Source Server Type    : PostgreSQL
 Source Server Version : 160012 (160012)
 Source Host           : localhost:5432
 Source Catalog        : xrcgs_roadsafety
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 160012 (160012)
 File Encoding         : 65001

 Date: 04/04/2026 00:35:48
*/


-- ----------------------------
-- Table structure for road_inspection_handling_detail
-- ----------------------------
DROP TABLE IF EXISTS "public"."road_inspection_handling_detail";
CREATE TABLE "public"."road_inspection_handling_detail" (
  "id" int8 NOT NULL,
  "record_id" int8 NOT NULL,
  "category_code" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "category_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "detail_text" text COLLATE "pg_catalog"."default" NOT NULL,
  "detail_order" int4 NOT NULL DEFAULT 0,
  "created_at" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
)
;

-- ----------------------------
-- Table structure for road_inspection_photo
-- ----------------------------
DROP TABLE IF EXISTS "public"."road_inspection_photo";
CREATE TABLE "public"."road_inspection_photo" (
  "id" int8 NOT NULL,
  "record_id" int8 NOT NULL,
  "file_id" int8 NOT NULL,
  "description" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "sort_order" int4 NOT NULL DEFAULT 0,
  "created_at" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
)
;

-- ----------------------------
-- Table structure for road_inspection_record
-- ----------------------------
DROP TABLE IF EXISTS "public"."road_inspection_record";
CREATE TABLE "public"."road_inspection_record" (
  "id" int8 NOT NULL,
  "record_date" date NOT NULL,
  "unit_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "weather" varchar(50) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "patrol_team" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "patrol_vehicle" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "location" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "inspection_content" text COLLATE "pg_catalog"."default",
  "issues_found" text COLLATE "pg_catalog"."default",
  "handling_situation_raw" text COLLATE "pg_catalog"."default",
  "handover_summary" text COLLATE "pg_catalog"."default",
  "remark" text COLLATE "pg_catalog"."default",
  "created_by" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "created_at" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  "updated_at" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  "exported_by" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "exported_at" timestamp(6),
  "export_file_name" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "approval_status" varchar(32) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'UNSUBMITTED'::character varying,
  "squad_code" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "form_payload_json" text COLLATE "pg_catalog"."default",
  "details_payload_json" text COLLATE "pg_catalog"."default",
  "summary_payload_json" text COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- View structure for v_road_inspection_log_echo
-- ----------------------------
DROP VIEW IF EXISTS "public"."v_road_inspection_log_echo";
CREATE VIEW "public"."v_road_inspection_log_echo" AS  WITH detail_agg AS (
         SELECT d_1.record_id,
            jsonb_agg(COALESCE(NULLIF(d_1.detail_text, ''::text)::jsonb, '{}'::jsonb) || jsonb_build_object('_detail_id', d_1.id, '_detail_order', d_1.detail_order, '_category_code', d_1.category_code, '_category_name', d_1.category_name, '_created_at', d_1.created_at) ORDER BY d_1.detail_order, d_1.id) AS details_json
           FROM road_inspection_handling_detail d_1
          GROUP BY d_1.record_id
        ), photo_agg AS (
         SELECT p_1.record_id,
            jsonb_agg(jsonb_build_object('id', p_1.id, 'fileId', p_1.file_id, 'url', '/api/file/preview/'::text || p_1.file_id::text, 'caption', p_1.description, 'sortOrder', p_1.sort_order, 'createdAt', p_1.created_at) ORDER BY p_1.sort_order, p_1.id) AS photos_json
           FROM road_inspection_photo p_1
          GROUP BY p_1.record_id
        )
 SELECT r.id AS record_id,
    r.record_date,
    r.squad_code,
    r.unit_name,
    r.weather,
    r.patrol_team,
    r.patrol_vehicle,
    r.location,
    r.inspection_content,
    r.issues_found,
    r.handling_situation_raw,
    r.handover_summary,
    r.remark,
    r.approval_status,
    r.created_by,
    r.created_at,
    r.updated_at,
    r.exported_by,
    r.exported_at,
    r.export_file_name,
    COALESCE(d.details_json, '[]'::jsonb) AS details_json,
    COALESCE(p.photos_json, '[]'::jsonb) AS photos_json,
    COALESCE(NULLIF(r.form_payload_json, ''::text)::jsonb, jsonb_build_object('date', to_char(r.record_date::timestamp with time zone, 'YYYY-MM-DD'::text), 'team', r.squad_code, 'unitName', r.unit_name, 'weather', r.weather, 'vehicle', r.patrol_vehicle, 'remark', r.remark)) || jsonb_build_object('details', COALESCE(NULLIF(r.details_payload_json, ''::text)::jsonb, COALESCE(d.details_json, '[]'::jsonb)), 'photos', COALESCE(p.photos_json, '[]'::jsonb), 'summaryPayload', COALESCE(NULLIF(r.summary_payload_json, ''::text)::jsonb, '{}'::jsonb)) AS replay_payload_json
   FROM road_inspection_record r
     LEFT JOIN detail_agg d ON d.record_id = r.id
     LEFT JOIN photo_agg p ON p.record_id = r.id;
COMMENT ON VIEW "public"."v_road_inspection_log_echo" IS '巡查日志三表全字段展示';

-- ----------------------------
-- Indexes structure for table road_inspection_handling_detail
-- ----------------------------
CREATE INDEX "idx_rihd_record_id_created_at" ON "public"."road_inspection_handling_detail" USING btree (
  "record_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST,
  "detail_order" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table road_inspection_handling_detail
-- ----------------------------
ALTER TABLE "public"."road_inspection_handling_detail" ADD CONSTRAINT "road_inspection_handling_detail_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table road_inspection_photo
-- ----------------------------
CREATE INDEX "idx_rip_record_id_created_at" ON "public"."road_inspection_photo" USING btree (
  "record_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST,
  "sort_order" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table road_inspection_photo
-- ----------------------------
ALTER TABLE "public"."road_inspection_photo" ADD CONSTRAINT "road_inspection_photo_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table road_inspection_record
-- ----------------------------
CREATE UNIQUE INDEX "uk_record_date_squad_code" ON "public"."road_inspection_record" USING btree (
  "record_date" "pg_catalog"."date_ops" ASC NULLS LAST,
  "squad_code" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
COMMENT ON INDEX "public"."uk_record_date_squad_code" IS '日期和中队唯一键';

-- ----------------------------
-- Primary Key structure for table road_inspection_record
-- ----------------------------
ALTER TABLE "public"."road_inspection_record" ADD CONSTRAINT "road_inspection_record_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table road_inspection_handling_detail
-- ----------------------------
ALTER TABLE "public"."road_inspection_handling_detail" ADD CONSTRAINT "fk_handling_record" FOREIGN KEY ("record_id") REFERENCES "public"."road_inspection_record" ("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- ----------------------------
-- Foreign Keys structure for table road_inspection_photo
-- ----------------------------
ALTER TABLE "public"."road_inspection_photo" ADD CONSTRAINT "fk_photo_record" FOREIGN KEY ("record_id") REFERENCES "public"."road_inspection_record" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
