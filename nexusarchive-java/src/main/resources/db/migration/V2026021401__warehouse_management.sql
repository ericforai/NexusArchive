-- =====================================================
-- 库房管理系统 - 数据库迁移脚本
-- =====================================================
-- 版本: V1.0.0
-- 创建时间: 2026-02-13
-- 描述: 创建库房管理相关表，支持档案柜、档案袋、盘点、借阅功能
-- 兼容性: 与现有数据库架构保持一致
-- =====================================================

-- =====================================================
-- 1. 档案柜表 (archives_cabinet)
-- =====================================================
-- 管理物理档案柜的基本信息
-- =====================================================
CREATE TABLE archives_cabinet (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,         -- 柜号：C-01, C-02...
    name VARCHAR(100),                        -- 柜名称
    location VARCHAR(200),                      -- 存放位置
    rows INT DEFAULT 5,                         -- 层数
    columns INT DEFAULT 4,                      -- 列数
    row_capacity INT DEFAULT 25,                -- 每列容量
    total_capacity INT GENERATED ALWAYS AS (rows * columns * row_capacity) STORED,
    current_count INT DEFAULT 0,                 -- 当前档案袋数量
    status VARCHAR(20) DEFAULT 'normal',        -- normal/disabled/full
    fonds_id BIGINT REFERENCES sys_fonds(id),
    remark VARCHAR(500),
    created_by BIGINT REFERENCES sys_user(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE archives_cabinet IS '档案柜表';
COMMENT ON COLUMN archives_cabinet.code IS '柜号：C-01, C-02... 按全宗内顺序编号';
COMMENT ON COLUMN archives_cabinet.status IS '状态：normal-正常/disabled-停用/full-已满';

CREATE INDEX idx_cabinet_fonds ON archives_cabinet(fonds_id);
CREATE INDEX idx_cabinet_status ON archives_cabinet(status);

-- =====================================================
-- 2. 档案袋表 (archives_container)
-- =====================================================
-- 档案袋是实物档案的基本存储单位
-- 一个档案袋可关联多个电子案卷，支持 RFID/二维码定位
-- =====================================================
CREATE TABLE archives_container (
    id BIGSERIAL PRIMARY KEY,
    container_no VARCHAR(50) NOT NULL,           -- 袋号：CN-YYYY-NNN
    cabinet_id BIGINT REFERENCES archives_cabinet(id),
    cabinet_position VARCHAR(100),             -- 柜内位置
    physical_location VARCHAR(200),           -- 物理定位：RFID/二维码
    volume_id BIGINT,                         -- 关联的电子案卷ID（可为空）
    capacity INT DEFAULT 50,                 -- 袋容量（盒数）
    archive_count INT DEFAULT 0,             -- 已装盒档案数量
    status VARCHAR(20) DEFAULT 'empty',       -- empty/normal/full/damaged/borrowed
    check_status VARCHAR(20) DEFAULT 'pending', -- pending/checked/unchecked
    last_inventory_id BIGINT,               -- 最近盘点任务ID
    last_inventory_time TIMESTAMP,             -- 最近盘点时间
    last_inventory_result VARCHAR(50),        -- 盘点结果
    fonds_id BIGINT REFERENCES sys_fonds(id),
    created_by BIGINT REFERENCES sys_user(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE archives_container IS '档案袋表';
COMMENT ON COLUMN archives_container.container_no IS '袋号格式：CN-YYYY-NNN';
COMMENT ON COLUMN archives_container.status IS '状态：empty-空袋/normal-正常/full-满袋/damaged-已损坏/borrowed-借出/pending-待盘点';
COMMENT ON COLUMN archives_container.check_status IS '盘点状态：pending-待盘点/checked-已盘点/unchecked-未盘点';

CREATE INDEX idx_container_cabinet ON archives_container(cabinet_id);
CREATE INDEX idx_container_status ON archives_container(status);
CREATE INDEX idx_container_volume ON archives_container(volume_id);

-- =====================================================
-- 3. 档案袋-案卷关联表 (archives_container_volume)
-- =====================================================
-- 支持一个档案袋关联多个案卷
-- 通过 is_primary 标记主卷用于排序显示
-- =====================================================
CREATE TABLE archives_container_volume (
    id BIGSERIAL PRIMARY KEY,
    container_id BIGINT REFERENCES archives_container(id) ON DELETE CASCADE,
    volume_id BIGINT REFERENCES acc_archive_volume(id),
    is_primary BOOLEAN DEFAULT TRUE,           -- 是否主卷（用于排序）
    display_order INT DEFAULT 0,            -- 显示顺序
    boxed_at TIMESTAMP,                         -- 装盒时间
    boxed_by BIGINT REFERENCES sys_user(id),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (container_id, volume_id)
);

COMMENT ON TABLE archives_container_volume IS '档案袋-案卷关联表';
COMMENT ON COLUMN archives_container_volume.is_primary IS '是否主卷：用于排序和显示';

CREATE INDEX idx_cv_container ON archives_container_volume(container_id);
CREATE INDEX idx_cv_volume ON archives_container_volume(volume_id);

-- =====================================================
-- 4. 盘点任务表 (archives_inventory)
-- =====================================================
-- 管理档案盘点任务和执行过程
-- =====================================================
CREATE TABLE archives_inventory (
    id BIGSERIAL PRIMARY KEY,
    task_no VARCHAR(50) NOT NULL UNIQUE,    -- 任务号：PD-YYYY-NNN
    task_name VARCHAR(100),                   -- 任务名称
    cabinet_id BIGINT,                       -- 盘点档案柜（可为空，空=全库盘点）
    start_cabinet_code VARCHAR(50),            -- 起始柜号
    end_cabinet_code VARCHAR(50),              -- 结束柜号
    status VARCHAR(20) DEFAULT 'pending',      -- pending/in_progress/completed/cancelled
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    total_containers INT DEFAULT 0,             -- 盘点档案袋总数
    checked_containers INT DEFAULT 0,           -- 已盘点数量
    abnormal_containers INT DEFAULT 0,          -- 异常数量
    fonds_id BIGINT REFERENCES sys_fonds(id),
    created_by BIGINT REFERENCES sys_user(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE archives_inventory IS '盘点任务表';
COMMENT ON COLUMN archives_inventory.status IS '状态：pending-待开始/in_progress-进行中/completed-已完成/cancelled-已取消';

CREATE INDEX idx_inventory_cabinet ON archives_inventory(cabinet_id);
CREATE INDEX idx_inventory_status ON archives_inventory(status);
CREATE INDEX idx_inventory_fonds ON archives_inventory(fonds_id);

-- =====================================================
-- 5. 盘点明细表 (archives_inventory_detail)
-- =====================================================
-- 记录每个档案袋的盘点结果
-- =====================================================
CREATE TABLE archives_inventory_detail (
    id BIGSERIAL PRIMARY KEY,
    inventory_id BIGINT REFERENCES archives_inventory(id) ON DELETE CASCADE,
    container_id BIGINT REFERENCES archives_container(id),
    expected_status VARCHAR(20),              -- 预期：normal/damaged/missing/extra
    actual_status VARCHAR(20),               -- 实际：normal/damaged/missing/extra
    difference VARCHAR(20),                   -- 差异：matched/missing/extra/damaged
    remark VARCHAR(500),
    fonds_id BIGINT REFERENCES sys_fonds(id),
    created_by BIGINT REFERENCES sys_user(id),
    created_at TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE archives_inventory_detail IS '盘点明细表';
COMMENT ON COLUMN archives_inventory_detail.difference IS '差异：matched-一致/missing-缺失/extra-多余/damaged-损坏';

CREATE INDEX idx_inventory_detail_container ON archives_inventory_detail(container_id);

-- =====================================================
-- 6. 实物借阅表 (archives_borrowing)
-- =====================================================
-- 管理实物档案的借阅和归还
-- =====================================================
CREATE TABLE archives_borrowing (
    id BIGSERIAL PRIMARY KEY,
    borrow_no VARCHAR(50) NOT NULL UNIQUE,   -- 借阅单号：BW-YYYY-NNN
    container_id BIGINT REFERENCES archives_container(id),
    borrower VARCHAR(100) NOT NULL,            -- 借阅人
    borrower_dept VARCHAR(200),                  -- 借阅部门
    borrow_date DATE NOT NULL,                -- 借阅日期
    expected_return_date DATE,                 -- 预计归还日期
    status VARCHAR(20) DEFAULT 'borrowed',     -- borrowed/returned/overdue
    actual_return_date DATE,                    -- 实际归还日期
    approved_by BIGINT REFERENCES sys_user(id),      -- 审批人
    approved_at TIMESTAMP,                     -- 审批时间
    remark VARCHAR(500),
    fonds_id BIGINT REFERENCES sys_fonds(id),
    created_by BIGINT REFERENCES sys_user(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE archives_borrowing IS '实物借阅表';
COMMENT ON COLUMN archives_borrowing.status IS '状态：borrowed-已借出/returned-已归还/overdue-逾期未还';

CREATE INDEX idx_borrowing_container ON archives_borrowing(container_id);
CREATE INDEX idx_borrowing_status ON archives_borrowing(status);
CREATE INDEX idx_borrowing_borrower ON archives_borrowing(borrower);
CREATE INDEX idx_borrowing_date ON archives_borrowing(borrow_date);

-- =====================================================
-- 7. 扩展电子档案表
-- =====================================================
-- 为支持实物档案管理，扩展 acc_archive 表
-- =====================================================

-- 添加实物状态字段
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS physical_status VARCHAR(20) DEFAULT 'in_stock';

COMMENT ON COLUMN acc_archive.physical_status IS '实物档案状态：in_stock-在库/borrowed/lost';

-- 添加实物位置字段
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS physical_location VARCHAR(200);

COMMENT ON COLUMN acc_archive.physical_location IS '实物存放位置：格式为柜号-层-位，如 C01-03-02';

-- 创建索引以支持实物档案查询
CREATE INDEX idx_archive_physical_status ON acc_archive(physical_status);
CREATE INDEX idx_archive_physical_location ON acc_archive(physical_location);

-- =====================================================
-- 8. 权限数据初始化（库房管理相关权限）
-- =====================================================
-- 插入库房管理菜单权限
-- =====================================================

-- 确保权限表存在
INSERT INTO sys_permission (code, name, category, description, created_at, updated_at)
VALUES
    ('warehouse:cabinet:view', '库房管理-档案柜查看', 'warehouse', '查看档案柜', NOW(), NOW()),
    ('warehouse:cabinet:manage', '库房管理-档案柜管理', 'warehouse', '管理档案柜', NOW(), NOW()),
    ('warehouse:container:view', '库房管理-档案袋查看', 'warehouse', '查看档案袋', NOW(), NOW()),
    ('warehouse:container:manage', '库房管理-档案袋管理', 'warehouse', '管理档案袋', NOW(), NOW()),
    ('warehouse:container:link', '库房管理-案卷关联', 'warehouse', '关联案卷', NOW(), NOW()),
    ('warehouse:inventory:view', '库房管理-盘点查看', 'warehouse', '查看盘点任务', NOW(), NOW()),
    ('warehouse:inventory:manage', '库房管理-盘点任务', 'warehouse', '管理盘点任务', NOW(), NOW()),
    ('warehouse:inventory:execute', '库房管理-执行盘点', 'warehouse', '执行盘点', NOW(), NOW()),
    ('warehouse:borrowing:view', '库房管理-借阅查看', 'warehouse', '查看借阅记录', NOW(), NOW()),
    ('warehouse:borrowing:approve', '库房管理-借阅审批', 'warehouse', '审批借阅申请', NOW(), NOW()),
    ('warehouse:borrowing:manage', '库房管理-借阅管理', 'warehouse', '管理借阅', NOW(), NOW())
    ('warehouse:borrowing:return', '库房管理-归还确认', 'warehouse', '确认归还', NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

-- 为管理员角色分配库房管理权限
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id as role_id, p.id as permission_id, NOW(), NOW()
FROM sys_role r
CROSS JOIN sys_permission p ON p.category = 'warehouse'
WHERE r.code IN ('system_admin', 'security_admin', 'audit_admin', 'business_user');

-- =====================================================
-- 9. 完成迁移脚本
-- =====================================================

-- 迁移完成
DO $$;
