# ERP 集成配置

## 概述

NexusArchive 支持多种 ERP 系统对接，通过统一适配器接口实现凭证同步。

---

## 支持的 ERP 系统

| ERP 类型 | 适配器 | 状态 |
|:---|:---|:---|
| 用友 YonSuite | YonSuiteAdapter | ✅ 已实现 |
| 金蝶云星空 | KingdeeAdapter | ✅ 已实现 |
| 通用 HTTP API | GenericErpAdapter | ✅ 已实现 |

---

## 配置管理

### API 接口

| 方法 | 端点 | 说明 |
|:---|:---|:---|
| GET | `/api/erp/config` | 获取所有配置 |
| POST | `/api/erp/config` | 新增/更新配置 |
| DELETE | `/api/erp/config/{id}` | 删除配置 |
| GET | `/api/erp/config/types` | 获取支持的类型 |

### 配置示例

#### 金蝶云星空

```json
{
  "name": "金蝶K3生产环境",
  "erpType": "KINGDEE",
  "configJson": {
    "host": "https://api.kingdee.com",
    "dbId": "your_db_id",
    "username": "your_username",
    "password": "encrypted_password",
    "acctId": "accounting_id"
  },
  "isActive": 1
}
```

#### 通用 HTTP API

```json
{
  "name": "自研ERP系统",
  "erpType": "GENERIC",
  "configJson": {
    "apiUrl": "https://erp.company.com/api/vouchers",
    "authType": "BEARER",
    "authToken": "your_token",
    "mappingRule": {
      "voucherCode": "$.data.voucherNo",
      "voucherDate": "$.data.date"
    }
  },
  "isActive": 1
}
```

---

## 适配器接口

所有适配器实现统一接口：

```java
public interface ErpAdapter {
    String getType();
    boolean testConnection(Map<String, String> config);
    ErpPageResponse<ErpVoucherDto> pullVouchers(ErpPageRequest request, Map<String, String> config);
    ErpVoucherDto getVoucherDetail(String externalId, Map<String, String> config);
}
```

---

## 扩展新 ERP

1. 实现 `ErpAdapter` 接口
2. 添加 `@Component` 注解
3. 工厂自动注册

```java
@Component
public class CustomErpAdapter implements ErpAdapter {
    @Override
    public String getType() { return "CUSTOM"; }
    // ... 实现其他方法
}
```

---

## 安全注意事项

1. **密码加密** - configJson 中的密码应使用 SM4 加密存储
2. **权限控制** - ERP 配置管理需要 ADMIN 角色
3. **审计日志** - 所有配置变更均记录审计日志
