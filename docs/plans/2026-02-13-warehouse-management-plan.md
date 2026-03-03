# 库房管理系统实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标：** 实现电子+实物双轨制的库房管理系统，管理档案柜、档案袋、盘点、借阅等实物档案操作。

---

## 架构概览

**技术栈：** Spring Boot 3.1.6, MyBatis-Plus 3.5.7, PostgreSQL, React 19, TypeScript

**核心原则：**
- 遵循 DDD 分层架构
- 前后端分离，通过 REST API 交互
- 实物档案与电子档案通过 `volume_id` 关联
- 支持全宗隔离

---

## Phase 1: 数据库设计与迁移 (1-2 天)

### 任务列表

| ID | 任务 | 文件 | 预计时间 |
|----|------|------|----------|
| P1-1 | 创建数据库迁移脚本 V2026021401 | `nexusarchive-java/src/main/resources/db/migration/V2026021401__warehouse_management.sql` | 2小时 |
| P1-2 | 创建档案柜实体 Entity | `ArchiveCabinet.java` | 1小时 |
| P1-3 | 创建档案袋实体 Entity | `ArchiveContainer.java` | 1小时 |
| P1-4 | 创建案卷关联实体 Entity | `ContainerVolume.java` | 0.5小时 |
| P1-5 | 创建盘点实体 Entity | `ArchiveInventory.java`, `ArchiveInventoryDetail.java` | 1小时 |
| P1-6 | 创建借阅实体 Entity | `ArchiveBorrowing.java` | 1小时 |
| P1-7 | 扩展电子档案表字段 | ALTER TABLE 语句 | 0.5小时 |

### 数据库表设计

#### 1. 档案柜表 (archives_cabinet)

```sql
CREATE TABLE archives_cabinet (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100),
    location VARCHAR(200),
    rows INT DEFAULT 5,
    columns INT DEFAULT 4,
    row_capacity INT DEFAULT 25,
    total_capacity INT GENERATED ALWAYS AS (rows * columns * row_capacity) STORED,
    current_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'normal',
    fonds_id BIGINT REFERENCES sys_fonds(id),
    remark VARCHAR(500),
    created_by BIGINT REFERENCES sys_user(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

#### 2. 档案袋表 (archives_container)

```sql
CREATE TABLE archives_container (
    id BIGSERIAL PRIMARY KEY,
    container_no VARCHAR(50) NOT NULL,
    cabinet_id BIGINT REFERENCES archives_cabinet(id),
    cabinet_position VARCHAR(100),
    physical_location VARCHAR(200),
    volume_id BIGINT,
    capacity INT DEFAULT 50,
    archive_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'empty',
    check_status VARCHAR(20) DEFAULT 'pending',
    last_inventory_id BIGINT,
    last_inventory_time TIMESTAMP,
    last_inventory_result VARCHAR(50),
    fonds_id BIGINT REFERENCES sys_fonds(id),
    created_by BIGINT REFERENCES sys_user(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

#### 3. 档案袋-案卷关联表 (archives_container_volume)

```sql
CREATE TABLE archives_container_volume (
    id BIGSERIAL PRIMARY KEY,
    container_id BIGINT REFERENCES archives_container(id) ON DELETE CASCADE,
    volume_id BIGINT REFERENCES acc_archive_volume(id),
    is_primary BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0,
    boxed_at TIMESTAMP,
    boxed_by BIGINT REFERENCES sys_user(id),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (container_id, volume_id)
);
```

#### 4. 盘点相关表

```sql
-- 盘点任务表
CREATE TABLE archives_inventory (
    id BIGSERIAL PRIMARY KEY,
    task_no VARCHAR(50) NOT NULL UNIQUE,
    task_name VARCHAR(100),
    cabinet_id BIGINT,
    start_cabinet_code VARCHAR(50),
    end_cabinet_code VARCHAR(50),
    status VARCHAR(20) DEFAULT 'pending',
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    total_containers INT DEFAULT 0,
    checked_containers INT DEFAULT 0,
    abnormal_containers INT DEFAULT 0,
    fonds_id BIGINT REFERENCES sys_fonds(id),
    created_by BIGINT REFERENCES sys_user(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 盘点明细表
CREATE TABLE archives_inventory_detail (
    id BIGSERIAL PRIMARY KEY,
    inventory_id BIGINT REFERENCES archives_inventory(id) ON DELETE CASCADE,
    container_id BIGINT REFERENCES archives_container(id),
    expected_status VARCHAR(20),
    actual_status VARCHAR(20),
    difference VARCHAR(20),
    remark VARCHAR(500),
    fonds_id BIGINT REFERENCES sys_fonds(id),
    created_by BIGINT REFERENCES sys_user(id),
    created_at TIMESTAMP DEFAULT NOW()
);
```

#### 5. 实物借阅表 (archives_borrowing)

```sql
CREATE TABLE archives_borrowing (
    id BIGSERIAL PRIMARY KEY,
    borrow_no VARCHAR(50) NOT NULL UNIQUE,
    container_id BIGINT REFERENCES archives_container(id),
    borrower VARCHAR(100) NOT NULL,
    borrower_dept VARCHAR(200),
    borrow_date DATE NOT NULL,
    expected_return_date DATE,
    status VARCHAR(20) DEFAULT 'borrowed',
    actual_return_date DATE,
    approved_by BIGINT REFERENCES sys_user(id),
    approved_at TIMESTAMP,
    remark VARCHAR(500),
    fonds_id BIGINT REFERENCES sys_fonds(id),
    created_by BIGINT REFERENCES sys_user(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

#### 6. 电子档案表扩展

```sql
-- 扩展 acc_archive 表
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS physical_status VARCHAR(20) DEFAULT 'in_stock';
ALTER TABLE acc_archive ADD COLUMN IF NOT EXISTS physical_location VARCHAR(200);

-- 创建索引
CREATE INDEX idx_archive_physical_status ON acc_archive(physical_status);
CREATE INDEX idx_archive_physical_location ON acc_archive(physical_location);
```

---

## Phase 2: 后端开发 (3-5 天)

### 任务列表

| ID | 任务 | 文件 | 预计时间 |
|----|------|------|----------|
| P2-1 | 创建档案柜 Entity + Mapper + Service + Controller | `ArchiveCabinet*.java` | 1天 |
| P2-2 | 创建档案袋 Entity + Mapper + Service + Controller | `ArchiveContainer*.java` | 1.5天 |
| P2-3 | 创建案卷关联 Entity + Mapper | `ContainerVolume*.java` | 0.5天 |
| P2-4 | 创建盘点 Entity + Mapper + Service + Controller | `ArchiveInventory*.java` | 1.5天 |
| P2-5 | 创建借阅 Entity + Mapper + Service + Controller | `ArchiveBorrowing*.java` | 1.5天 |
| P2-6 | 创建 DTO 和 VO 类 | `warehouse/dto/*.java` | 0.5天 |
| P2-7 | 编号生成服务 | `CodeGeneratorService.java` | 0.5天 |
| P2-8 | 档案袋-案卷关联 Service | `ContainerVolumeService.java` | 0.5天 |

### 核心业务逻辑

#### 档案柜编码规则
- 格式：`C-{全宗代码}-{2位序号}`
- 示例：`C-01`, `C-02`
- 自动递增，跨全宗独立

#### 档案袋编码规则
- 格式：`CN-{YYYY}-{4位流水号}`
- 示例：`CN-2024-0001`
- 按年度+全宗独立流水

#### 借阅单编码规则
- 格式：`BW-{YYYY}-{4位流水号}`
- 示例：`BW-2024-0001`

### 文件结构

```
nexusarchive-java/src/main/java/com/nexusarchive/
├── entity/
│   ├── warehouse/
│   │   ├── ArchiveCabinet.java
│   │   ├── ArchiveContainer.java
│   │   ├── ArchiveInventory.java
│   │   ├── ArchiveInventoryDetail.java
│   │   ├── ContainerVolume.java
│   │   └── ArchiveBorrowing.java
├── dto/
│   └── warehouse/
│       ├── CabinetDTO.java
│       ├── ContainerDTO.java
│       ├── VolumeLinkDTO.java
│       ├── ContainerVO.java
│       ├── ContainerDetailVO.java
│       ├── InventoryDTO.java
│       ├── BorrowingDTO.java
│       └── BorrowingVO.java
├── mapper/
│   └── warehouse/
│       ├── ArchiveCabinetMapper.java
│       ├── ArchiveContainerMapper.java
│       ├── ContainerVolumeMapper.java
│       ├── ArchiveInventoryMapper.java
│       ├── ArchiveInventoryDetailMapper.java
│       └── ArchiveBorrowingMapper.java
├── service/
│   └── warehouse/
│       ├── ArchiveCabinetService.java
│       ├── ArchiveContainerService.java
│       ├── ContainerVolumeService.java
│       ├── ArchiveInventoryService.java
│       ├── ArchiveBorrowingService.java
│       └── CodeGeneratorService.java
└── controller/
    └── warehouse/
        ├── ArchiveCabinetController.java
        ├── ArchiveContainerController.java
        ├── ArchiveInventoryController.java
        └── ArchiveBorrowingController.java
```

---

## Phase 3: 前端开发 (3-5 天)

### 任务列表

| ID | 任务 | 文件 | 预计时间 |
|----|------|------|----------|
| P3-1 | 创建仓库模块目录结构 | `src/pages/warehouse/` | 0.5小时 |
| P3-2 | 档案柜管理视图 | `CabinetListView.tsx` | 1.5天 |
| P3-3 | 档案袋管理视图 | `ContainerListView.tsx` | 2天 |
| P3-4 | 档案袋详情抽屉 | `ContainerDetailDrawer.tsx` | 1.5天 |
| P3-5 | 案卷关联弹窗 | `VolumeLinkModal.tsx` | 1天 |
| P3-6 | 盘点任务视图 | `InventoryListView.tsx` | 1.5天 |
| P3-7 | 盘点执行界面 | `InventoryProcessView.tsx` | 2天 |
| P3-8 | 借阅管理视图 | `BorrowingListView.tsx` | 1.5天 |
| P3-9 | 借阅申请表单 | `BorrowingForm.tsx` | 1天 |
| P3-10 | 更新路由和菜单配置 | `routes/paths.ts`, `constants.tsx` | 0.5小时 |

### 前端路由设计

```typescript
// src/routes/paths.ts
export const ROUTE_PATHS = {
    // ... existing paths
    WAREHOUSE: '/system/warehouse',
    WAREHOUSE_CABINETS: '/system/warehouse/cabinets',
    WAREHOUSE_CONTAINERS: '/system/warehouse/containers',
    WAREHOUSE_INVENTORY: '/system/warehouse/inventory',
    WAREHOUSE_BORROWING: '/system/warehouse/borrowing',
};

export const SUBITEM_TO_PATH: Record<string, string> = {
    // ... existing mappings
    '档案柜': ROUTE_PATHS.WAREHOUSE_CABINETS,
    '档案袋': ROUTE_PATHS.WAREHOUSE_CONTAINERS,
    '档案盘点': ROUTE_PATHS.WAREHOUSE_INVENTORY,
    '实物借阅': ROUTE_PATHS.WAREHOUSE_BORROWING,
};
```

### 页面组件架构

```typescript
// src/pages/warehouse/WarehouseView.tsx (主容器)
import { useSearchParams } from 'react-router-dom';

type WarehouseViewMode = 'cabinets' | 'containers' | 'inventory' | 'borrowing' | null;

export const WarehouseView: React.FC = () => {
  const [searchParams] = useSearchParams();
  const view = (searchParams.get('view') || null) as WarehouseViewMode;

  return (
    <div className="h-full flex flex-col bg-slate-50">
      {view === 'cabinets' && <CabinetListView />}
      {view === 'containers' && <ContainerListView />}
      {view === 'inventory' && <InventoryListView />}
      {view === 'borrowing' && <BorrowingListView />}
      {view === null && <EmptyView onNavigateBack={() => window.location.href = '/system'} />}
    </div>
  );
};
```

---

## Phase 4: 测试与联调 (2-3 天)

### 任务列表

| ID | 任务 | 描述 | 预计时间 |
|----|------|------|----------|
| P4-1 | Entity 单元测试 | `ArchiveCabinetTest.java` | 1天 |
| P4-2 | Mapper 集成测试 | `ArchiveCabinetMapperTest.java` | 0.5天 |
| P4-3 | Service 层测试 | `ArchiveCabinetServiceTest.java` | 1天 |
| P4-4 | Controller API 测试 | `ArchiveCabinetControllerTest.java` | 0.5天 |
| P4-5 | 前端组件测试 | `CabinetList.test.tsx` | 1天 |
| P4-6 | 前后端联调测试 | 1天 |

---

## Phase 5: 文档更新 (0.5天)

| ID | 任务 | 描述 | 预计时间 |
|----|------|------|----------|
| P5-1 | 更新 README.md | 1小时 |
| P5-2 | 创建数据库设计文档 | 1小时 |
| P5-3 | 更新 API 文档 | 1小时 |

---

## 依赖检查

- [ ] MyBatis-Plus 配置正确
- [ ] PostgreSQL 版本兼容
- [ ] 前端路由懒加载配置
- [ ] 权限注解 `@PreAuthorize("nav:warehouse")`

---

## 风险评估

| 风险 | 等级 | 缓解措施 |
|------|--------|----------|
| 数据迁移失败 | 高 | 先在测试环境验证，回滚机制 |
| 前后端接口不一致 | 中 | 严格的 DTO 定义和 API 文档 |
| 全宗隔离问题 | 中 | Service 层强制过滤 fonds_id |
| 性能问题 | 低 | 分页查询，合理索引 |

---

## 下一步

Phase 1 开始条件：
- [ ] 设计文档已确认
- [ ] 数据库迁移脚本已创建

**等待用户确认后开始执行。**
