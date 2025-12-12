-- V23__add_signature_log.sql
-- 签章日志表
-- 
-- 合规要求:
-- - DA/T 94-2022: 电子会计档案元数据规范
-- - 记录所有签章/验签操作以便审计追溯
--
-- @author Agent B - 合规开发工程师

CREATE TABLE IF NOT EXISTS arc_signature_log (
    id VARCHAR(32) PRIMARY KEY,
    archive_id VARCHAR(32) NOT NULL,
    file_id VARCHAR(32),
    signer_name VARCHAR(100),
    signer_cert_sn VARCHAR(100),
    signer_org VARCHAR(200),
    sign_time TIMESTAMP,
    sign_algorithm VARCHAR(20) DEFAULT 'SM2',
    signature_value TEXT,
    verify_result VARCHAR(20),
    verify_time TIMESTAMP,
    verify_message TEXT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引：按档案ID查询签章记录
CREATE INDEX IF NOT EXISTS idx_signature_archive ON arc_signature_log(archive_id);

-- 索引：按文件ID查询签章记录
CREATE INDEX IF NOT EXISTS idx_signature_file ON arc_signature_log(file_id);

-- 索引：按验证结果筛选
CREATE INDEX IF NOT EXISTS idx_signature_verify_result ON arc_signature_log(verify_result);

-- 添加注释
COMMENT ON TABLE arc_signature_log IS '签章日志表 - 记录电子签章/验签操作';
COMMENT ON COLUMN arc_signature_log.id IS '主键ID';
COMMENT ON COLUMN arc_signature_log.archive_id IS '关联的档案ID';
COMMENT ON COLUMN arc_signature_log.file_id IS '关联的文件ID';
COMMENT ON COLUMN arc_signature_log.signer_name IS '签章人姓名';
COMMENT ON COLUMN arc_signature_log.signer_cert_sn IS '证书序列号';
COMMENT ON COLUMN arc_signature_log.signer_org IS '签章单位';
COMMENT ON COLUMN arc_signature_log.sign_time IS '签章时间';
COMMENT ON COLUMN arc_signature_log.sign_algorithm IS '签名算法(SM2/RSA)';
COMMENT ON COLUMN arc_signature_log.signature_value IS '签名值(Base64)';
COMMENT ON COLUMN arc_signature_log.verify_result IS '验证结果(VALID/INVALID/UNKNOWN)';
COMMENT ON COLUMN arc_signature_log.verify_time IS '验证时间';
COMMENT ON COLUMN arc_signature_log.verify_message IS '验证消息';
COMMENT ON COLUMN arc_signature_log.created_time IS '创建时间';
