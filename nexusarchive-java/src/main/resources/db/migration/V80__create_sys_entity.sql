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
    name VARCHAR(255) NOT NULL COMMENT '法人名称',
    tax_id VARCHAR(50) COMMENT '统一社会信用代码/税号',
    address VARCHAR(500) COMMENT '注册地址',
    contact_person VARCHAR(100) COMMENT '联系人',
    contact_phone VARCHAR(50) COMMENT '联系电话',
    contact_email VARCHAR(100) COMMENT '联系邮箱',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, INACTIVE',
    description VARCHAR(1000) COMMENT '描述',
    created_by VARCHAR(64) COMMENT '创建人ID',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除'
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_entity_status ON sys_entity(status);
CREATE INDEX IF NOT EXISTS idx_entity_tax_id ON sys_entity(tax_id);
CREATE INDEX IF NOT EXISTS idx_entity_deleted ON sys_entity(deleted);

-- 表注释
COMMENT ON TABLE sys_entity IS '法人实体表（管理维度，不作为数据隔离键）';

-- ----------------------------
-- 全宗表添加 entity_id 字段
-- ----------------------------
-- 关联全宗与法人（一个法人可对应多个全宗）
ALTER TABLE bas_fonds ADD COLUMN IF NOT EXISTS entity_id VARCHAR(64) COMMENT '所属法人ID';
CREATE INDEX IF NOT EXISTS idx_fonds_entity_id ON bas_fonds(entity_id);

COMMENT ON COLUMN bas_fonds.entity_id IS '所属法人ID（管理维度，不作为数据隔离键）';

