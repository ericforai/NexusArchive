-- Input: 数据库引擎
-- Output: sys_entity 表创建脚本
-- Pos: Flyway 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- ----------------------------
-- 法人实体表 (Entity Management)
-- ----------------------------
-- PRD 来源: Section 1.1 - 法人仅管理维度
-- 说明: entity_id 仅用于治理、统计与合规台账，不作为数据隔离键
CREATE TABLE IF NOT EXISTS sys_entity (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    tax_id VARCHAR(50),
    address VARCHAR(500),
    contact_person VARCHAR(100),
    contact_phone VARCHAR(50),
    contact_email VARCHAR(100),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    description VARCHAR(1000),
    created_by VARCHAR(64),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_entity_status ON sys_entity(status);
CREATE INDEX IF NOT EXISTS idx_entity_tax_id ON sys_entity(tax_id);
CREATE INDEX IF NOT EXISTS idx_entity_deleted ON sys_entity(deleted);

-- 列注释
COMMENT ON TABLE sys_entity IS '法人实体表（管理维度，不作为数据隔离键）';
COMMENT ON COLUMN sys_entity.name IS '法人名称';
COMMENT ON COLUMN sys_entity.tax_id IS '统一社会信用代码/税号';
COMMENT ON COLUMN sys_entity.address IS '注册地址';
COMMENT ON COLUMN sys_entity.contact_person IS '联系人';
COMMENT ON COLUMN sys_entity.contact_phone IS '联系电话';
COMMENT ON COLUMN sys_entity.contact_email IS '联系邮箱';
COMMENT ON COLUMN sys_entity.status IS '状态: ACTIVE, INACTIVE';
COMMENT ON COLUMN sys_entity.description IS '描述';
COMMENT ON COLUMN sys_entity.created_by IS '创建人ID';
COMMENT ON COLUMN sys_entity.created_time IS '创建时间';
COMMENT ON COLUMN sys_entity.updated_time IS '更新时间';
COMMENT ON COLUMN sys_entity.deleted IS '逻辑删除: 0=未删除, 1=已删除';

-- ----------------------------
-- 全宗表添加 entity_id 字段
-- ----------------------------
-- 关联全宗与法人（一个法人可对应多个全宗）
ALTER TABLE bas_fonds ADD COLUMN IF NOT EXISTS entity_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_fonds_entity_id ON bas_fonds(entity_id);

COMMENT ON COLUMN bas_fonds.entity_id IS '所属法人ID（管理维度，不作为数据隔离键）';

-- 创建触发器函数用于自动更新 updated_time
CREATE OR REPLACE FUNCTION update_sys_entity_updated_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 创建触发器
DROP TRIGGER IF EXISTS trigger_update_sys_entity_updated_time ON sys_entity;
CREATE TRIGGER trigger_update_sys_entity_updated_time
    BEFORE UPDATE ON sys_entity
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_entity_updated_time();
