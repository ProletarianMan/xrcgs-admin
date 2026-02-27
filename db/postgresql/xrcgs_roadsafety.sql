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

 Date: 27/02/2026 12:24:15
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
  "squad_code" varchar(64) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Primary Key structure for table road_inspection_handling_detail
-- ----------------------------
ALTER TABLE "public"."road_inspection_handling_detail" ADD CONSTRAINT "road_inspection_handling_detail_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table road_inspection_photo
-- ----------------------------
ALTER TABLE "public"."road_inspection_photo" ADD CONSTRAINT "road_inspection_photo_pkey" PRIMARY KEY ("id");

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
