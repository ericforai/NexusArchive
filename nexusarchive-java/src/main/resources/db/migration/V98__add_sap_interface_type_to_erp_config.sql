-- Input: sys_erp_config table
-- Output: Add sap_interface_type column
-- Pos: src/main/resources/db/migration/
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- ================================================================
-- Migration: V98__add_sap_interface_type_to_erp_config.sql
-- Purpose: 添加 SAP 接口类型字段到 ERP 配置表
-- Author: Claude Code
-- Date: 2026-01-11
-- ================================================================

BEGIN;

-- 添加 SAP 接口类型字段
ALTER TABLE public.sys_erp_config
ADD COLUMN IF NOT EXISTS sap_interface_type VARCHAR(20);

COMMENT ON COLUMN public.sys_erp_config.sap_interface_type IS 'SAP接口类型: ODATA, RFC, IDOC, GATEWAY (仅当erp_type=SAP时有效)';

COMMIT;
