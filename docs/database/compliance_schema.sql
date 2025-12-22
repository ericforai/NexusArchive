-- Input: 数据库引擎
-- Output: 数据库结构初始化/变更
-- Pos: 数据库初始化脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- ----------------------------
-- 1. 全宗管理 (Fonds Management)
-- ----------------------------
-- 依据 DA/T 94-2022，全宗是档案管理的顶层单位
CREATE TABLE IF NOT EXISTS bas_fonds (
    id VARCHAR(32) PRIMARY KEY,
    fonds_code VARCHAR(50) NOT NULL UNIQUE,
    fonds_name VARCHAR(100) NOT NULL,
    company_name VARCHAR(100),
    description VARCHAR(500),
    created_by VARCHAR(32),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE bas_fonds IS '全宗基础信息表';
COMMENT ON COLUMN bas_fonds.fonds_code IS '全宗号 (Fonds Code)';
COMMENT ON COLUMN bas_fonds.fonds_name IS '全宗名称 (Fonds Name)';
COMMENT ON COLUMN bas_fonds.company_name IS '立档单位名称 (Constituting Unit)';
COMMENT ON COLUMN bas_fonds.description IS '描述';
COMMENT ON COLUMN bas_fonds.created_by IS '创建人ID';

-- ----------------------------
-- 2. 档号计数器 (Archival Code Sequence)
-- ----------------------------
-- 用于生成严格连续的档号，防止并发重号
CREATE TABLE IF NOT EXISTS sys_archival_code_sequence (
    fonds_code VARCHAR(50) NOT NULL,
    fiscal_year VARCHAR(4) NOT NULL,
    category_code VARCHAR(10) NOT NULL,
    current_val INT DEFAULT 0,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (fonds_code, fiscal_year, category_code)
);

COMMENT ON TABLE sys_archival_code_sequence IS '档号生成计数器';
COMMENT ON COLUMN sys_archival_code_sequence.fonds_code IS '全宗号';
COMMENT ON COLUMN sys_archival_code_sequence.fiscal_year IS '会计年度';
COMMENT ON COLUMN sys_archival_code_sequence.category_code IS '档案类别 (AC01/AC02...)';
COMMENT ON COLUMN sys_archival_code_sequence.current_val IS '当前流水号';

-- ----------------------------
-- 3. 档案表扩展 (Archive Extension)
-- ----------------------------
-- 关联全宗ID
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS fonds_id VARCHAR(32);
COMMENT ON COLUMN acc_archive.fonds_id IS '所属全宗ID';

-- 索引
CREATE INDEX IF NOT EXISTS idx_archive_fonds_id ON acc_archive(fonds_id);

-- ----------------------------
-- 4. 审计巡检表扩展 (Audit Log Extension)
-- ----------------------------
-- 固化四性检测报告
ALTER TABLE audit_inspection_log ADD COLUMN IF NOT EXISTS report_file_path VARCHAR(500);
ALTER TABLE audit_inspection_log ADD COLUMN IF NOT EXISTS report_file_hash VARCHAR(100);

COMMENT ON COLUMN audit_inspection_log.report_file_path IS '检测报告物理文件路径(XML)';
COMMENT ON COLUMN audit_inspection_log.report_file_hash IS '检测报告文件哈希';
