# 电子会计档案系统 - 历史数据迁移工具 OpenSpec

> **版本**: v1.0  
> **日期**: 2025-01  
> **对齐基准**: [开发路线图 v1.0](../planning/development_roadmap_v1.0.md)  
> **优先级**: **P0（阻塞性问题 - 必须立即解决）**  
> **当前状态**: 待开发

---

## 📊 功能概览

| 功能模块 | 路线图章节 | 优先级 | 预计工作量 | 依赖关系 |
|---------|-----------|--------|-----------|---------|
| 历史数据迁移工具 | 阶段四：数据迁移工具 | **P0** | 2-3 周 | 无 |

---

## 🎯 业务目标

**用户故事**: 作为系统管理员，我需要将历史档案数据（CSV/Excel 格式）批量导入系统，系统应自动识别并创建全宗结构，支持数据验证、错误报告和事务回滚，确保数据导入的准确性和可追溯性。

**业务价值**:
- 支持系统上线前的历史数据迁移
- 降低人工录入成本
- 确保数据导入的准确性和完整性
- 提供导入过程的可追溯性

---

## 📋 功能范围

### 1. CSV/Excel 文件导入

#### 1.1 文件格式支持

**支持的格式**:
- CSV（UTF-8 编码，支持 BOM）
- Excel（.xlsx, .xls）

**必需字段**（最低要求）:
| 字段名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| fonds_no | String | 是 | 全宗号 | JD-001 |
| fonds_name | String | 是 | 全宗名称 | 京东集团 |
| entity_name | String | 否 | 法人实体名称 | 北京京东世纪贸易有限公司 |
| entity_tax_code | String | 否 | 统一社会信用代码 | 91110000MA01234567 |
| archive_year | Integer | 是 | 归档年度 | 2024 |
| doc_type | String | 是 | 档案类型 | 凭证、报表、账簿等 |
| title | String | 是 | 档案标题 | 2024年1月记账凭证 |
| doc_date | Date | 否 | 形成日期 | 2024-01-15 |
| amount | Decimal | 否 | 金额 | 1000000.00 |
| counterparty | String | 否 | 对方单位 | 供应商A |
| voucher_no | String | 否 | 凭证号 | V-202401-001 |
| invoice_no | String | 否 | 发票号 | INV-202401-001 |
| retention_policy_name | String | 是 | 保管期限名称 | 永久、30年、10年等 |

**可选字段**:
- `custom_metadata` (JSON String): 扩展元数据
- `file_path` (String): 文件路径（用于关联实际文件）
- `file_hash` (String): 文件哈希值（SM3/SHA256）

#### 1.2 文件解析与验证

**实现要求**:
- 支持大文件分块读取（避免内存溢出）
- 文件格式自动识别（通过文件扩展名和 Magic Number）
- 字段映射配置（支持自定义列名映射）
- 数据验证（字段格式、必填项、业务规则）
- 错误收集与报告生成

**技术规格**:

```java
@Service
@Transactional(rollbackFor = Exception.class)
public class LegacyImportService {
    /**
     * 导入历史数据
     * 
     * @param file 上传的文件（CSV 或 Excel）
     * @param mappingConfig 字段映射配置（可选，如不提供则使用默认映射）
     * @param operatorId 操作人ID
     * @param fondsNo 当前操作人的全宗号（用于权限校验）
     * @return 导入结果
     */
    ImportResult importLegacyData(MultipartFile file, 
                                   FieldMappingConfig mappingConfig,
                                   String operatorId, 
                                   String fondsNo);
    
    /**
     * 预览导入数据（不执行导入，仅解析和验证）
     * 
     * @param file 上传的文件
     * @param mappingConfig 字段映射配置
     * @return 预览结果（包含解析的数据、验证错误、统计信息）
     */
    ImportPreviewResult previewImport(MultipartFile file, 
                                       FieldMappingConfig mappingConfig);
    
    /**
     * 验证单行数据
     */
    ValidationResult validateRow(ImportRow row, int rowNumber);
    
    /**
     * 批量导入（支持事务回滚）
     */
    ImportResult batchImport(List<ImportRow> validRows, 
                            String operatorId, 
                            String fondsNo);
}
```

**数据模型**:

```java
public class ImportResult {
    private String importId;  // 导入任务ID
    private int totalRows;  // 总行数
    private int successRows;  // 成功导入行数
    private int failedRows;  // 失败行数
    private List<ImportError> errors;  // 错误列表
    private List<String> createdFondsNos;  // 自动创建的全宗号列表
    private List<String> createdEntityIds;  // 自动创建的实体ID列表
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ImportStatus status;  // SUCCESS, PARTIAL_SUCCESS, FAILED
}

public class ImportError {
    private int rowNumber;  // 行号（从1开始，包含表头）
    private String fieldName;  // 字段名
    private String errorCode;  // 错误代码
    private String errorMessage;  // 错误消息
}

public class FieldMappingConfig {
    // 字段映射：CSV/Excel 列名 -> 系统字段名
    private Map<String, String> fieldMappings;
    // 默认使用标准字段名（无需映射）
}
```

### 2. 全宗结构自动生成

#### 2.1 全宗自动创建逻辑

**实现要求**:
- 根据导入数据中的 `fonds_no` 自动创建全宗（如果不存在）
- 根据 `entity_name` 和 `entity_tax_code` 自动创建法人实体（如果不存在）
- 全宗与实体自动关联
- 支持全宗名称和实体信息的去重匹配

**技术规格**:

```java
public class FondsAutoCreationService {
    /**
     * 自动创建或获取全宗
     * 
     * @param fondsNo 全宗号
     * @param fondsName 全宗名称
     * @param entityName 法人实体名称（可选）
     * @param entityTaxCode 统一社会信用代码（可选）
     * @param operatorId 操作人ID
     * @return 全宗ID（已存在则返回现有ID，不存在则创建后返回）
     */
    String ensureFondsExists(String fondsNo, 
                             String fondsName,
                             String entityName,
                             String entityTaxCode,
                             String operatorId);
    
    /**
     * 自动创建或获取法人实体
     * 
     * @param entityName 法人实体名称
     * @param entityTaxCode 统一社会信用代码
     * @return 实体ID
     */
    String ensureEntityExists(String entityName, String entityTaxCode);
    
    /**
     * 全宗与实体关联
     */
    void associateFondsWithEntity(String fondsId, String entityId);
}
```

**业务规则**:
1. **全宗创建规则**:
   - 如果 `fonds_no` 已存在，使用现有全宗
   - 如果 `fonds_no` 不存在，创建新全宗
   - 全宗状态默认为 `ACTIVE`
   - 全宗有效期为 `valid_from = 当前日期`, `valid_to = NULL`（永久有效）

2. **实体创建规则**:
   - 优先使用 `entity_tax_code` 进行匹配（精确匹配）
   - 如果 `entity_tax_code` 为空，使用 `entity_name` 进行模糊匹配
   - 如果实体不存在，创建新实体
   - 实体状态默认为 `ACTIVE`

3. **关联规则**:
   - 如果导入数据中包含实体信息，自动关联全宗与实体
   - 如果实体信息为空，全宗可以不关联实体（允许后续手动关联）

### 3. 数据验证与错误处理

#### 3.1 数据验证规则

**字段级验证**:
- `fonds_no`: 必须符合格式规范（字母数字下划线，长度 1-50）
- `archive_year`: 必须是有效年份（1900-2100）
- `doc_type`: 必须在允许的类型列表中
- `title`: 不能为空，长度限制 1-255
- `amount`: 必须是有效数字（>= 0）
- `doc_date`: 必须是有效日期格式
- `retention_policy_name`: 必须在系统中存在，或使用默认值

**业务规则验证**:
- 同一批导入中，`fonds_no` 必须一致（或允许跨全宗导入需特殊权限）
- `archive_year` 必须与 `doc_date` 的年份一致（或允许差异需说明）
- 必填字段不能为空

**技术规格**:

```java
public class ImportValidationService {
    /**
     * 验证单行数据
     */
    ValidationResult validateRow(ImportRow row, int rowNumber, ValidationContext context);
    
    /**
     * 验证全宗号格式
     */
    boolean validateFondsNo(String fondsNo);
    
    /**
     * 验证归档年度
     */
    boolean validateArchiveYear(Integer archiveYear);
    
    /**
     * 验证保管期限
     */
    String resolveRetentionPolicyId(String retentionPolicyName);
}
```

#### 3.2 错误处理与报告

**实现要求**:
- 收集所有验证错误（不中断导入过程）
- 生成详细的错误报告（包含行号、字段名、错误原因）
- 支持错误报告的导出（Excel/CSV）
- 支持部分成功导入（成功行导入，失败行记录错误）

**错误报告格式**:

```java
public class ImportErrorReport {
    private String importId;
    private List<ImportError> errors;
    private Map<String, Integer> errorStatistics;  // 错误类型统计
    private byte[] exportToExcel();  // 导出为 Excel
    private byte[] exportToCsv();  // 导出为 CSV
}
```

### 4. 事务管理与回滚

#### 4.1 事务策略

**实现要求**:
- 整个导入过程在一个事务中执行
- 如果发生任何错误，支持全部回滚
- 支持分批导入（每批 1000 条，避免事务过大）
- 记录导入日志用于审计

**技术规格**:

```java
@Service
@Transactional(rollbackFor = Exception.class)
public class LegacyImportServiceImpl implements LegacyImportService {
    
    @Override
    public ImportResult importLegacyData(MultipartFile file, 
                                         FieldMappingConfig mappingConfig,
                                         String operatorId, 
                                         String fondsNo) {
        // 1. 解析文件
        List<ImportRow> rows = parseFile(file, mappingConfig);
        
        // 2. 验证数据
        List<ValidationResult> validationResults = validateRows(rows);
        
        // 3. 分离成功和失败的行
        List<ImportRow> validRows = filterValidRows(rows, validationResults);
        List<ImportError> errors = collectErrors(validationResults);
        
        // 4. 批量导入（分批执行，每批 1000 条）
        int successCount = 0;
        for (List<ImportRow> batch : Lists.partition(validRows, 1000)) {
            successCount += batchImport(batch, operatorId, fondsNo);
        }
        
        // 5. 记录审计日志
        auditLogService.logImport(importId, operatorId, fondsNo, successCount, errors.size());
        
        // 6. 返回结果
        return buildImportResult(importId, rows.size(), successCount, errors);
    }
}
```

### 5. 导入日志与审计

#### 5.1 导入日志记录

**实现要求**:
- 记录每次导入操作的详细信息
- 包含：操作人、导入时间、文件信息、导入结果
- 支持导入历史查询
- 支持导入日志导出

**数据模型**:

```sql
-- 导入任务表
CREATE TABLE IF NOT EXISTS legacy_import_task (
    id VARCHAR(32) PRIMARY KEY,
    operator_id VARCHAR(32) NOT NULL,
    operator_name VARCHAR(100),
    fonds_no VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_hash VARCHAR(64),  -- 文件哈希值
    total_rows INT NOT NULL,
    success_rows INT NOT NULL DEFAULT 0,
    failed_rows INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,  -- PENDING, PROCESSING, SUCCESS, FAILED, PARTIAL_SUCCESS
    error_report_path VARCHAR(500),  -- 错误报告文件路径
    created_fonds_nos TEXT,  -- JSON 数组：自动创建的全宗号列表
    created_entity_ids TEXT,  -- JSON 数组：自动创建的实体ID列表
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_import_task_operator (operator_id, created_at),
    INDEX idx_import_task_fonds (fonds_no, created_at),
    INDEX idx_import_task_status (status, created_at)
);

COMMENT ON TABLE legacy_import_task IS '历史数据导入任务表';
COMMENT ON COLUMN legacy_import_task.status IS '状态: PENDING(待处理), PROCESSING(处理中), SUCCESS(成功), FAILED(失败), PARTIAL_SUCCESS(部分成功)';
```

---

## 🔧 API 设计

### 5.1 后端 API

#### 5.1.1 `POST /api/legacy-import/preview`

**用途**: 预览导入数据（不执行导入，仅解析和验证）

**请求**:
```http
POST /api/legacy-import/preview
Content-Type: multipart/form-data

file: <file>
mappingConfig: <optional JSON>
```

**响应**:
```json
{
  "code": 200,
  "message": "预览成功",
  "data": {
    "totalRows": 1000,
    "validRows": 950,
    "invalidRows": 50,
    "previewData": [
      {
        "rowNumber": 1,
        "data": {
          "fonds_no": "JD-001",
          "fonds_name": "京东集团",
          "archive_year": 2024,
          "doc_type": "凭证",
          "title": "2024年1月记账凭证"
        },
        "validationErrors": []
      }
    ],
    "errors": [
      {
        "rowNumber": 51,
        "fieldName": "fonds_no",
        "errorCode": "REQUIRED_FIELD_MISSING",
        "errorMessage": "全宗号不能为空"
      }
    ],
    "statistics": {
      "fondsCount": 5,
      "entityCount": 3,
      "willCreateFonds": ["JD-002", "JD-003"],
      "willCreateEntities": ["北京京东世纪贸易有限公司"]
    }
  }
}
```

#### 5.1.2 `POST /api/legacy-import/import`

**用途**: 执行数据导入

**请求**:
```http
POST /api/legacy-import/import
Content-Type: multipart/form-data

file: <file>
mappingConfig: <optional JSON>
```

**响应**:
```json
{
  "code": 200,
  "message": "导入完成",
  "data": {
    "importId": "import-20250115-001",
    "totalRows": 1000,
    "successRows": 950,
    "failedRows": 50,
    "status": "PARTIAL_SUCCESS",
    "createdFondsNos": ["JD-002", "JD-003"],
    "createdEntityIds": ["entity-001", "entity-002"],
    "errors": [
      {
        "rowNumber": 51,
        "fieldName": "fonds_no",
        "errorCode": "REQUIRED_FIELD_MISSING",
        "errorMessage": "全宗号不能为空"
      }
    ],
    "errorReportUrl": "/api/legacy-import/import-20250115-001/error-report",
    "startTime": "2025-01-15T10:00:00",
    "endTime": "2025-01-15T10:05:30"
  }
}
```

#### 5.1.3 `GET /api/legacy-import/tasks`

**用途**: 查询导入历史

**请求**:
```http
GET /api/legacy-import/tasks?page=1&size=20&status=SUCCESS
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "total": 100,
    "list": [
      {
        "id": "import-20250115-001",
        "operatorName": "张三",
        "fileName": "历史数据.xlsx",
        "fileSize": 1048576,
        "totalRows": 1000,
        "successRows": 950,
        "failedRows": 50,
        "status": "PARTIAL_SUCCESS",
        "createdAt": "2025-01-15T10:00:00",
        "completedAt": "2025-01-15T10:05:30"
      }
    ]
  }
}
```

#### 5.1.4 `GET /api/legacy-import/tasks/{importId}/error-report`

**用途**: 下载错误报告

**响应**: Excel 文件（Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet）

---

### 5.2 前端界面

#### 5.2.1 导入页面组件

**文件位置**: `src/pages/admin/LegacyImportPage.tsx`

**功能**:
- 文件上传（支持拖拽）
- 导入预览（显示解析结果和验证错误）
- 执行导入（显示进度）
- 导入历史查询
- 错误报告下载

**UI 流程**:
1. 用户上传文件
2. 点击"预览"按钮，系统解析文件并显示预览结果
3. 用户查看验证错误（如有）
4. 用户点击"确认导入"按钮
5. 系统执行导入，显示进度条
6. 导入完成后显示结果摘要
7. 如有错误，提供错误报告下载链接

---

## 📦 数据库变更

### 6.1 新增表

```sql
-- 导入任务表（已在 5.1 节定义）
CREATE TABLE IF NOT EXISTS legacy_import_task (...);
```

### 6.2 数据迁移脚本

**文件位置**: `nexusarchive-java/src/main/resources/db/migration/VXXX__create_legacy_import_task.sql`

---

## 🧪 测试要求

### 7.1 单元测试

- `LegacyImportService` 单元测试
- `FondsAutoCreationService` 单元测试
- `ImportValidationService` 单元测试
- CSV/Excel 解析器单元测试

### 7.2 集成测试

- 完整导入流程测试（成功场景）
- 部分失败场景测试（验证事务回滚）
- 全宗自动创建测试
- 实体自动创建测试
- 错误报告生成测试

### 7.3 测试数据

- 准备标准的 CSV 测试文件（包含各种场景）
- 准备 Excel 测试文件
- 准备包含错误的测试文件（用于验证错误处理）

---

## 📝 开发检查清单

### 后端开发

- [ ] 创建 `LegacyImportService` 接口和实现类
- [ ] 实现 CSV 解析器（使用 Apache Commons CSV）
- [ ] 实现 Excel 解析器（使用 Apache POI）
- [ ] 实现字段映射配置功能
- [ ] 实现数据验证服务
- [ ] 实现全宗自动创建服务
- [ ] 实现实体自动创建服务
- [ ] 实现批量导入逻辑（支持事务）
- [ ] 创建导入任务表（数据库迁移脚本）
- [ ] 实现导入日志记录
- [ ] 实现错误报告生成（Excel/CSV）
- [ ] 创建 `LegacyImportController`
- [ ] 编写单元测试
- [ ] 编写集成测试
- [ ] 更新 API 文档

### 前端开发

- [ ] 创建 `LegacyImportPage.tsx`
- [ ] 实现文件上传组件
- [ ] 实现导入预览组件
- [ ] 实现导入进度显示
- [ ] 实现导入历史列表
- [ ] 实现错误报告下载
- [ ] 编写组件测试
- [ ] 编写 E2E 测试

---

## 🔗 相关文档

- 开发路线图：`docs/planning/development_roadmap_v1.0.md`
- 缺口分析报告：`docs/reports/roadmap-gap-analysis-2025-01.md`
- 全宗管理相关服务：`nexusarchive-java/src/main/java/com/nexusarchive/service/BasFondsService.java`
- 实体管理相关服务：`nexusarchive-java/src/main/java/com/nexusarchive/service/EntityService.java`

---

**文档状态**: ✅ 已完成  
**下一步**: 开始开发实现





