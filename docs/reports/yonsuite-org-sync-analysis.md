# YonSuite 组织同步接口分析报告

> 分析日期: 2026-01-12
> 目的: 验证当前 YonSuite 组织同步实现是否使用了正确的接口

---

## 一、当前实现分析

### 1.1 使用的 API

**YonSuiteOrgClient.java** (nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/client/YonSuiteOrgClient.java:60-82)

```java
// 步骤1: 获取树版本
POST /yonbip/digitalModel/openapi/treedatasync/treeversionsync
{
  "pubts": "1970-01-01 00:00:00",
  "funcCode": "adminorg",
  "pageIndex": 1,
  "pageSize": 100
}

// 步骤2: 获取组织成员
POST /yonbip/digitalModel/openapi/treedatasync/treemembersync
{
  "pubts": "1970-01-01 00:00:00",
  "treeVids": ["tree-id"],
  "pageIndex": 1,
  "pageSize": 100
}
```

### 1.2 funcCode 参数说明

当前使用 `funcCode: "adminorg"` (行政组织)

根据 YonSuite 官方文档，可能的 funcCode 值：
- `adminorg` - 行政组织（包含部门、员工关系）
- `orgunit` - 组织单元（仅组织架构）

---

## 二、问题识别

### 2.1 致命问题：同步未完成保存

**ErpOrgSyncService.java:55**
```java
result.setMessage(String.format("同步完成: 发现 %d 条组织数据（保存功能待实现）", orgRecords.size()));
```

**现状**: 代码从 YonSuite 获取了组织数据，但**没有保存到 sys_entity 表**！

### 2.2 API 选择验证

根据 YonSuite 官方资料（https://developer.yonyou.com/cloud/integrationAsset/assetDetail/1718875476673712130）：

> 基于yonBIPR2，BIP获取组织数据处理数据。定时任务-获取全量组织数据【全量】，获取新增组织数据、获取修改组织数据、获取删除组织数据。

**结论**: `treedatasync` 系列接口是 YonSuite 官方推荐的组织数据同步接口。

### 2.3 funcCode 选择问题

对于**法人档案管理系统**，需要同步的是**法人实体**，而非内部部门结构：

| funcCode | 返回数据 | 适合场景 |
|----------|----------|----------|
| `adminorg` | 行政组织 + 部门 + 员工 | 集团内部管理 |
| `orgunit` | 组织单元（不含员工） | 组织架构管理 |

**建议**: 需要确认 YonSuite 中法人数据的存储方式。如果法人数据存储在其他功能模块，当前 API 可能不是最优选择。

---

## 三、官方 API 文档参考

根据搜索结果，YonSuite 组织同步相关资源：

| 资源 | URL |
|------|-----|
| 组织数据同步资产包 | https://developer.yonyou.com/cloud/integrationAsset/assetDetail/1718875476673712130 |
| 开放平台 API 文档 | https://developer.yonyou.com/openAPI |
| 帮助中心 | https://doc.yonisv.com/isv/ |
| API 市场（需登录） | https://c2.yonyoucloud.com/iuap-ipaas-base/ucf-wh/console-fe/open-home/index.html#/doc-center/docDes/api |

**注意**: 完整的 API 列表需要使用 YonBIP/YonSuite 账号登录后在帮助中心查看。

---

## 四、建议改进

### 4.1 立即修复：完成数据保存逻辑

```java
// ErpOrgSyncService.java 需要补充
@Transactional
@CacheEvict(value = "entityTree", allEntries = true)
public SyncResult syncFromYonSuite(String lastSyncTime) {
    List<YonOrgTreeSyncResponse.OrgRecord> orgRecords = yonSuiteOrgClient.queryOrgs(lastSyncTime);

    for (YonOrgTreeSyncResponse.OrgRecord record : orgRecords) {
        SysEntity entity = new SysEntity();
        entity.setId(record.getId());
        entity.setName(record.getName());
        entity.setParentId(record.getParentId());
        entity.setOrderNum(record.getOrderNum());
        entity.setStatus(record.getEnableStatus() == 1 ? "ACTIVE" : "INACTIVE");
        // ... 其他字段映射
        entityService.saveOrUpdate(entity);
    }

    return result;
}
```

### 4.2 需要确认的问题

1. **法人数据来源**: YonSuite 中法人数据是否存储在 `adminorg` 接口中？
2. **增量同步**: `pubts` 参数是否正确实现了增量同步机制？
3. **数据映射**: YonSuite 返回的字段与 sys_entity 表字段的映射关系是否完整？

---

## 五、结论

| 问题 | 状态 | 说明 |
|------|------|------|
| API 选择 | ✅ 正确 | `treedatasync` 是官方推荐的组织同步接口 |
| funcCode 选择 | ⚠️ 需确认 | `adminorg` 可能包含过多部门层级数据 |
| 数据保存 | ✅ 已实现 | 2026-01-12 补充完整保存逻辑 |
| 错误处理 | ✅ 存在 | 有完整的异常捕获和日志记录 |

**修复状态 (2026-01-12)**:
1. ✅ 高优先级: 实现数据保存逻辑 - **已完成**
   - `ErpOrgSyncService.syncFromYonSuite()` 现在会保存/更新数据到 sys_entity
   - 支持新增和更新判断
   - 保留手动设置的 taxId、address 等字段
2. 🟡 中优先级: 确认 YonSuite 中法人数据的具体存储位置 - **待确认**
3. 🟢 低优先级: 优化增量同步机制 - **待处理**

---

## 六、参考代码位置

| 文件 | 路径 |
|------|------|
| YonSuite Org Client | `integration/yonsuite/client/YonSuiteOrgClient.java` |
| ERP 同步服务 | `service/ErpOrgSyncService.java` |
| 组织控制器 | `controller/AdminOrgController.java` |
| 同步请求 DTO | `integration/yonsuite/dto/YonOrgTreeSyncRequest.java` |
| 同步响应 DTO | `integration/yonsuite/dto/YonOrgTreeSyncResponse.java` |
