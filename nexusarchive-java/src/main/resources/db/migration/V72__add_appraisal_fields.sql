-- Input: Destruction Appraisal Enhancement
-- Output: Schema change for appraisal list
-- Pos: db/migration/V72
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 1. 为销毁申请表添加鉴定相关字段
ALTER TABLE biz_destruction ADD COLUMN IF NOT EXISTS appraiser_id VARCHAR(32);
ALTER TABLE biz_destruction ADD COLUMN IF NOT EXISTS appraiser_name VARCHAR(100);
ALTER TABLE biz_destruction ADD COLUMN IF NOT EXISTS appraisal_date DATE;
ALTER TABLE biz_destruction ADD COLUMN IF NOT EXISTS appraisal_conclusion VARCHAR(20);
ALTER TABLE biz_destruction ADD COLUMN IF NOT EXISTS appraisal_comment TEXT;
ALTER TABLE biz_destruction ADD COLUMN IF NOT EXISTS appraisal_list_id VARCHAR(32);

COMMENT ON COLUMN biz_destruction.appraiser_id IS '鉴定人ID';
COMMENT ON COLUMN biz_destruction.appraiser_name IS '鉴定人姓名';
COMMENT ON COLUMN biz_destruction.appraisal_date IS '鉴定日期';
COMMENT ON COLUMN biz_destruction.appraisal_conclusion IS '鉴定结论: APPROVED(同意销毁), REJECTED(不同意销毁), DEFERRED(延期保管)';
COMMENT ON COLUMN biz_destruction.appraisal_comment IS '鉴定意见';
COMMENT ON COLUMN biz_destruction.appraisal_list_id IS '鉴定清单ID（用于关联鉴定清单记录）';

-- 2. 创建鉴定清单表（用于存储鉴定清单快照）
CREATE TABLE IF NOT EXISTS biz_appraisal_list (
    id VARCHAR(32) PRIMARY KEY,
    fonds_no VARCHAR(50) NOT NULL,
    archive_year INT NOT NULL,
    appraiser_id VARCHAR(32) NOT NULL,
    appraiser_name VARCHAR(100) NOT NULL,
    appraisal_date DATE NOT NULL,
    archive_ids TEXT NOT NULL,  -- JSON 数组，存储待鉴定档案ID列表
    archive_snapshot TEXT NOT NULL,  -- JSON 格式，存储档案元数据快照
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING(待提交), SUBMITTED(已提交)
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

COMMENT ON TABLE biz_appraisal_list IS '鉴定清单表';
COMMENT ON COLUMN biz_appraisal_list.archive_ids IS '待鉴定档案ID列表(JSON数组)';
COMMENT ON COLUMN biz_appraisal_list.archive_snapshot IS '档案元数据快照(JSON格式，包含档案基本信息、保管期限信息等)';

-- 3. 创建索引
CREATE INDEX IF NOT EXISTS idx_appraisal_list_fonds_year 
    ON biz_appraisal_list(fonds_no, archive_year, appraisal_date);
CREATE INDEX IF NOT EXISTS idx_appraisal_list_status 
    ON biz_appraisal_list(status, created_time);



