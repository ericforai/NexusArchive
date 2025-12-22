-- Input: Flyway 迁移引擎、数据库引擎
-- Output: 数据库结构/数据迁移
-- Pos: 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- =====================================================
-- 泊冉集团组织架构数据
-- 组织结构: 集团公司 -> 子公司 -> 部门
-- =====================================================

-- 先清除已有组织数据（如需保留历史数据请注释此行）
DELETE FROM sys_org WHERE deleted = 0;

-- =====================================================
-- 一级：集团公司
-- =====================================================
INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_GROUP', '泊冉集团有限公司', 'BR-GROUP', NULL, 'COMPANY', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- =====================================================
-- 修复外键关联: sys_user.department_id -> sys_org.id
-- =====================================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sys_user_department_id_fkey') THEN
        ALTER TABLE sys_user DROP CONSTRAINT sys_user_department_id_fkey;
    END IF;
    
    -- 只有当 sys_org 表存在时才添加新外键 (V54 上下文肯定是存在的)
    ALTER TABLE sys_user ADD CONSTRAINT sys_user_department_id_fkey 
    FOREIGN KEY (department_id) REFERENCES sys_org(id);
END $$;

-- =====================================================
-- 二级：子公司
-- =====================================================
-- 销售公司
INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_SALES', '泊冉销售有限公司', 'BR-SALES', 'ORG_BR_GROUP', 'COMPANY', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- 贸易公司
INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_TRADE', '泊冉国际贸易有限公司', 'BR-TRADE', 'ORG_BR_GROUP', 'COMPANY', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- 生产公司
INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_MFG', '泊冉制造有限公司', 'BR-MFG', 'ORG_BR_GROUP', 'COMPANY', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- =====================================================
-- 三级：集团总部部门
-- =====================================================
INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_GROUP_FIN', '财务管理部', 'BR-GROUP-FIN', 'ORG_BR_GROUP', 'DEPARTMENT', 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_GROUP_HR', '人力资源部', 'BR-GROUP-HR', 'ORG_BR_GROUP', 'DEPARTMENT', 11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_GROUP_IT', '信息技术部', 'BR-GROUP-IT', 'ORG_BR_GROUP', 'DEPARTMENT', 12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_GROUP_LEGAL', '法务合规部', 'BR-GROUP-LEGAL', 'ORG_BR_GROUP', 'DEPARTMENT', 13, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_GROUP_AUDIT', '审计监察部', 'BR-GROUP-AUDIT', 'ORG_BR_GROUP', 'DEPARTMENT', 14, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- =====================================================
-- 三级：销售公司部门
-- =====================================================
INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_SALES_DOM', '国内销售部', 'BR-SALES-DOM', 'ORG_BR_SALES', 'DEPARTMENT', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_SALES_INT', '海外销售部', 'BR-SALES-INT', 'ORG_BR_SALES', 'DEPARTMENT', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_SALES_MKT', '市场推广部', 'BR-SALES-MKT', 'ORG_BR_SALES', 'DEPARTMENT', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_SALES_FIN', '财务部', 'BR-SALES-FIN', 'ORG_BR_SALES', 'DEPARTMENT', 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- =====================================================
-- 三级：贸易公司部门
-- =====================================================
INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_TRADE_IMP', '进口业务部', 'BR-TRADE-IMP', 'ORG_BR_TRADE', 'DEPARTMENT', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_TRADE_EXP', '出口业务部', 'BR-TRADE-EXP', 'ORG_BR_TRADE', 'DEPARTMENT', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_TRADE_LOG', '物流仓储部', 'BR-TRADE-LOG', 'ORG_BR_TRADE', 'DEPARTMENT', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_TRADE_FIN', '财务部', 'BR-TRADE-FIN', 'ORG_BR_TRADE', 'DEPARTMENT', 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- =====================================================
-- 三级：生产公司部门
-- =====================================================
INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_MFG_PROD', '生产管理部', 'BR-MFG-PROD', 'ORG_BR_MFG', 'DEPARTMENT', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_MFG_QC', '质量控制部', 'BR-MFG-QC', 'ORG_BR_MFG', 'DEPARTMENT', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_MFG_RD', '研发技术部', 'BR-MFG-RD', 'ORG_BR_MFG', 'DEPARTMENT', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_MFG_SUPPLY', '采购供应部', 'BR-MFG-SUPPLY', 'ORG_BR_MFG', 'DEPARTMENT', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted)
VALUES ('ORG_BR_MFG_FIN', '财务部', 'BR-MFG-FIN', 'ORG_BR_MFG', 'DEPARTMENT', 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- =====================================================
-- 更新 admin 用户归属到集团财务部
-- =====================================================
UPDATE sys_user SET department_id = 'ORG_BR_GROUP_FIN', org_code = 'BR-GROUP' WHERE username = 'admin' AND deleted = 0;
