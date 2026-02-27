/*
 Navicat Premium Dump SQL

 Source Server         : t14p pg16
 Source Server Type    : PostgreSQL
 Source Server Version : 160012 (160012)
 Source Host           : localhost:5432
 Source Catalog        : xrcgs_admin
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 160012 (160012)
 File Encoding         : 65001

 Date: 27/02/2026 12:23:55
*/


-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_dept";
CREATE TABLE "public"."sys_dept" (
  "id" int8 NOT NULL,
  "parent_id" int8 NOT NULL DEFAULT 0,
  "path" varchar(512) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "code" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "status" int2 NOT NULL DEFAULT 1,
  "sort_no" int4 NOT NULL DEFAULT 0,
  "leader_user_id" int8,
  "phone" varchar(30) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "email" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "remark" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "create_by" int8,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "update_by" int8,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "del_flag" int2 NOT NULL DEFAULT 0
)
;

-- ----------------------------
-- Table structure for sys_dict_item
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_dict_item";
CREATE TABLE "public"."sys_dict_item" (
  "id" int8 NOT NULL,
  "type_code" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "label" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "value" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "sort" int4 DEFAULT 0,
  "ext" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "status" int2 DEFAULT 1,
  "dept_id" int8,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_dict_type";
CREATE TABLE "public"."sys_dict_type" (
  "id" int8 NOT NULL,
  "code" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "status" int2 DEFAULT 1,
  "remark" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;

-- ----------------------------
-- Table structure for sys_file
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_file";
CREATE TABLE "public"."sys_file" (
  "id" int8 NOT NULL,
  "biz_type" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "file_type" varchar(16) COLLATE "pg_catalog"."default" NOT NULL,
  "original_name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "ext" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "mime" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "size" int8 NOT NULL,
  "sha256" char(64) COLLATE "pg_catalog"."default" NOT NULL,
  "storage_path" varchar(512) COLLATE "pg_catalog"."default" NOT NULL,
  "preview_path" varchar(512) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "status" varchar(16) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'UPLOADED'::character varying,
  "error_msg" varchar(1024) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "dept_id" int8,
  "created_at" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "created_by" int8
)
;

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_menu";
CREATE TABLE "public"."sys_menu" (
  "id" int8 NOT NULL,
  "parent_id" int8 DEFAULT 0,
  "title" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "path" varchar(128) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "router_name" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "component" varchar(128) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "type" varchar(16) COLLATE "pg_catalog"."default" NOT NULL,
  "perms" varchar(128) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "icon" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "rank" int4 DEFAULT 0,
  "keep_alive" int2 DEFAULT 0,
  "show_parent" int2 DEFAULT 0,
  "visible" int2 DEFAULT 1,
  "status" int2 DEFAULT 1,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "del_flag" int2 DEFAULT 0
)
;

-- ----------------------------
-- Table structure for sys_op_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_op_log";
CREATE TABLE "public"."sys_op_log" (
  "id" int8 NOT NULL,
  "title" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "username" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "methodsign" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "httpmethod" varchar(16) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "uri" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "ip" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "querystring" varchar(1024) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "reqbody" text COLLATE "pg_catalog"."default",
  "respbody" text COLLATE "pg_catalog"."default",
  "success" bool DEFAULT true,
  "elapsedms" int8,
  "exceptionmsg" varchar(1024) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "createdat" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;

-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_permission";
CREATE TABLE "public"."sys_permission" (
    "id" int8 NOT NULL,
    "parent_id" int8 NOT NULL DEFAULT 0,
    "code" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
    "name" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
    "remark" varchar(255) COLLATE "pg_catalog"."default",
    "sort_no" int4 NOT NULL DEFAULT 0,
    "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
    "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
  )
  ;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_role";
CREATE TABLE "public"."sys_role" (
  "id" int8 NOT NULL,
  "code" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "dept_id" int8,
  "extra_dept_ids" jsonb,
  "status" int2 NOT NULL DEFAULT 1,
  "data_scope" varchar(32) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'ALL'::character varying,
  "data_scope_ext" jsonb,
  "remark" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "sort_no" int4 NOT NULL DEFAULT 0,
  "create_by" int8,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  "update_by" int8,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  "del_flag" int2 NOT NULL DEFAULT 0
)
;

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_role_menu";
CREATE TABLE "public"."sys_role_menu" (
  "role_id" int8 NOT NULL,
  "menu_id" int8 NOT NULL
)
;

-- ----------------------------
-- Table structure for sys_role_perm
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_role_perm";
CREATE TABLE "public"."sys_role_perm" (
  "id" int8 NOT NULL,
  "role_id" int8 NOT NULL,
  "perm_id" int8 NOT NULL
)
;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_user";
CREATE TABLE "public"."sys_user" (
  "id" int8 NOT NULL,
  "username" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "password" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "nickname" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "wechat_id" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "phone" varchar(32) COLLATE "pg_catalog"."default" DEFAULT NULL::character varying,
  "gender" int2,
  "dept_id" int8,
  "extra_dept_ids" jsonb,
  "data_scope" varchar(32) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'SELF'::character varying,
  "data_scope_ext" jsonb,
  "enabled" int2 DEFAULT 1,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_user_role";
CREATE TABLE "public"."sys_user_role" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "role_id" int8 NOT NULL
)
;

-- ----------------------------
-- Checks structure for table sys_dept
-- ----------------------------
ALTER TABLE "public"."sys_dept" ADD CONSTRAINT "chk_sys_dept_path_format" CHECK (path::text ~ '^(/[0-9]+)+/$'::text);

-- ----------------------------
-- Primary Key structure for table sys_dept
-- ----------------------------
ALTER TABLE "public"."sys_dept" ADD CONSTRAINT "sys_dept_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_dict_item
-- ----------------------------
ALTER TABLE "public"."sys_dict_item" ADD CONSTRAINT "sys_dict_item_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table sys_dict_type
-- ----------------------------
ALTER TABLE "public"."sys_dict_type" ADD CONSTRAINT "uk_sys_dict_type_code" UNIQUE ("code");

-- ----------------------------
-- Primary Key structure for table sys_dict_type
-- ----------------------------
ALTER TABLE "public"."sys_dict_type" ADD CONSTRAINT "sys_dict_type_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_file
-- ----------------------------
ALTER TABLE "public"."sys_file" ADD CONSTRAINT "sys_file_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_menu
-- ----------------------------
ALTER TABLE "public"."sys_menu" ADD CONSTRAINT "sys_menu_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_op_log
-- ----------------------------
ALTER TABLE "public"."sys_op_log" ADD CONSTRAINT "sys_op_log_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_permission
-- ----------------------------
ALTER TABLE "public"."sys_permission" ADD CONSTRAINT "sys_permission_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Checks structure for table sys_role
-- ----------------------------
ALTER TABLE "public"."sys_role" ADD CONSTRAINT "chk_role_datascope" CHECK (data_scope::text = ANY (ARRAY['ALL'::character varying, 'DEPT'::character varying, 'DEPT_AND_CHILD'::character varying, 'SELF'::character varying, 'CUSTOM'::character varying]::text[]));
ALTER TABLE "public"."sys_role" ADD CONSTRAINT "chk_role_status" CHECK (status = ANY (ARRAY[0, 1]));

-- ----------------------------
-- Primary Key structure for table sys_role
-- ----------------------------
ALTER TABLE "public"."sys_role" ADD CONSTRAINT "sys_role_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_role_menu
-- ----------------------------
ALTER TABLE "public"."sys_role_menu" ADD CONSTRAINT "sys_role_menu_pkey" PRIMARY KEY ("role_id", "menu_id");

-- ----------------------------
-- Primary Key structure for table sys_role_perm
-- ----------------------------
ALTER TABLE "public"."sys_role_perm" ADD CONSTRAINT "sys_role_perm_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_user
-- ----------------------------
ALTER TABLE "public"."sys_user" ADD CONSTRAINT "sys_user_pkey" PRIMARY KEY ("id");
ALTER TABLE "public"."sys_user" ADD CONSTRAINT "chk_sys_user_gender" CHECK ("gender" IS NULL OR "gender" = ANY (ARRAY[0, 1]));

-- ----------------------------
-- Primary Key structure for table sys_user_role
-- ----------------------------
ALTER TABLE "public"."sys_user_role" ADD CONSTRAINT "sys_user_role_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table sys_dict_item
-- ----------------------------
ALTER TABLE "public"."sys_dict_item" ADD CONSTRAINT "fk_di_type" FOREIGN KEY ("type_code") REFERENCES "public"."sys_dict_type" ("code") ON DELETE RESTRICT ON UPDATE RESTRICT;
