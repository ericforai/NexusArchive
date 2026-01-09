# Entity-DTO 映射指南

## 概述

本文档说明如何将 Entity 直接返回改为 DTO 返回，以隐藏敏感字段和大字段。

## 为什么需要 DTO 层

1. **安全性**：防止敏感信息（密码、哈希值、内部路径）泄露到前端
2. **性能**：避免返回大字段（二进制数据、长 JSON）导致响应体积过大
3. **灵活性**：API 接口与数据库结构解耦，便于重构
4. **可维护性**：统一的 DTO 转换逻辑，便于修改和测试

## 核心原则

| 原则 | 说明 |
|------|------|
| **绝不返回密码相关字段** | `passwordHash`, `salt`, `secret` 等 |
| **不返回内部校验字段** | `fixityValue`, `prevLogHash`, `logHash` 等 |
| **不返回二进制大字段** | `timestampToken`, `signValue`, 文件内容等 |
| **谨慎返回大 JSON 字段** | `customMetadata`, `standardMetadata`, `sourceData` 等 |
| **逻辑删除标记不返回** | `deleted` 字段 |

## 已创建的 DTO 类

### 1. ApiResponse (通用响应包装器)

**位置**: `/src/main/java/com/nexusarchive/dto/response/ApiResponse.java`

**功能**: 统一 API 响应格式，支持 traceId 追踪

```java
@Data
@Builder
public class ApiResponse<T> {
    private String code;           // 响应码
    private String message;        // 响应消息
    private T data;                // 响应数据（DTO）
    private String traceId;        // 追踪ID
    private Long timestamp;        // 时间戳
}
```

**使用方式**:
```java
// 成功响应
ApiResponse.success(data)

// 失败响应
ApiResponse.error("错误消息")

// 带 traceId
ApiResponse.success(data).withTraceId(request)
```

### 2. ArchiveResponse (档案响应)

**位置**: `/src/main/java/com/nexusarchive/dto/response/ArchiveResponse.java`

**隐藏字段**:
- `customMetadata`: 自定义元数据（可能包含敏感信息）
- `standardMetadata`: 标准元数据（大 JSON 字段）
- `fixityValue`: 哈希值（内部校验用）
- `deleted`: 逻辑删除标记

**转换方法**:
```java
// 静态方法转换
ArchiveResponse dto = ArchiveResponse.fromEntity(entity);

// 使用 DtoMapper
ArchiveResponse dto = dtoMapper.toArchiveResponse(entity);
```

### 3. UserResponse (用户响应)

**位置**: `/src/main/java/com/nexusarchive/dto/response/UserResponse.java`

**隐藏字段**:
- `passwordHash`: 密码哈希（绝对不返回）
- `deleted`: 逻辑删除标记

**转换方法**:
```java
// 基础转换
UserResponse dto = UserResponse.fromEntity(entity);

// 带角色
UserResponse dto = UserResponse.fromEntity(entity, roleIds);

// 带角色和全宗
UserResponse dto = UserResponse.fromEntityWithFonds(entity, roleIds, fondsIds);
```

### 4. AuditLogResponse (审计日志响应)

**位置**: `/src/main/java/com/nexusarchive/dto/response/AuditLogResponse.java`

**隐藏字段**:
- `dataBefore`: 操作前数据（仅返回长度）
- `dataAfter`: 操作后数据（仅返回长度）
- `prevLogHash`: 前一条日志哈希（内部校验用）
- `logHash`: 当前日志哈希（内部校验用）

**转换方法**:
```java
// 基础转换
AuditLogResponse dto = AuditLogResponse.fromEntity(entity);

// 带变更摘要
AuditLogResponse dto = AuditLogResponse.fromEntityWithSummary(entity, "修改了档案标题");
```

### 5. FileContentResponse (文件内容响应)

**位置**: `/src/main/java/com/nexusarchive/dto/response/FileContentResponse.java`

**隐藏字段**:
- `timestampToken`: 时间戳令牌（二进制数据）
- `signValue`: 签名值（二进制数据）
- `certificate`: 数字证书（敏感信息）
- `storagePath`: 物理存储路径（内部信息）
- `sourceData`: 原始业务数据（可能包含敏感信息）

**转换方法**:
```java
// 基础转换
FileContentResponse dto = FileContentResponse.fromEntity(entity);

// 带签名状态
FileContentResponse dto = FileContentResponse.fromEntityWithSignature(entity, "VALID");
```

## DtoMapper 工具类

**位置**: `/src/main/java/com/nexusarchive/dto/mapper/DtoMapper.java`

**功能**: 提供统一的 Entity 到 DTO 转换方法

```java
@Component
public class DtoMapper {

    // Archive 相关
    public ArchiveResponse toArchiveResponse(Archive entity);
    public List<ArchiveResponse> toArchiveResponseList(List<Archive> entities);
    public Page<ArchiveResponse> toArchiveResponsePage(Page<Archive> entityPage);

    // User 相关
    public UserResponse toUserResponse(User entity);
    public List<UserResponse> toUserResponseList(List<User> entities);

    // AuditLog 相关
    public AuditLogResponse toAuditLogResponse(SysAuditLog entity);
    public Page<AuditLogResponse> toAuditLogResponsePage(Page<SysAuditLog> entityPage);

    // FileContent 相关
    public ArchiveFileResponse toArchiveFileResponse(ArcFileContent entity);
    public List<ArchiveFileResponse> toArchiveFileResponseList(List<ArcFileContent> entities);

    // 其他 Entity...
}
```

## Controller 使用示例

### 修改前（直接返回 Entity）

```java
@RestController
@RequestMapping("/archives")
public class ArchiveController {

    @GetMapping("/{id}")
    public Result<Archive> get(@PathVariable String id) {
        return Result.success(archiveService.getArchiveById(id));
    }
}
```

### 修改后（返回 DTO）

```java
@RestController
@RequestMapping("/archives")
@RequiredArgsConstructor
public class ArchiveController {

    private final DtoMapper dtoMapper;

    @GetMapping("/{id}")
    public ApiResponse<ArchiveResponse> get(@PathVariable String id) {
        Archive entity = archiveService.getArchiveById(id);
        return ApiResponse.success(dtoMapper.toArchiveResponse(entity));
    }

    @GetMapping
    public ApiResponse<Page<ArchiveResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        Page<Archive> entityPage = archiveService.getArchives(page, limit);
        Page<ArchiveResponse> dtoPage = dtoMapper.toArchiveResponsePage(entityPage);
        return ApiResponse.success(dtoPage);
    }
}
```

## 迁移检查清单

- [ ] 确认 Controller 返回类型从 Entity 改为 Response DTO
- [ ] 确认敏感字段已被正确隐藏
- [ ] 确认前端代码已更新以匹配新的响应格式
- [ ] 添加 API 文档（Swagger 注解）
- [ ] 编写单元测试验证转换逻辑

## 敏感字段参考表

| Entity | 敏感字段 | 处理方式 |
|--------|----------|----------|
| User | passwordHash | 完全不返回 |
| Archive | customMetadata | 不返回或返回摘要 |
| Archive | standardMetadata | 不返回或返回摘要 |
| Archive | fixityValue | 不返回 |
| ArcFileContent | timestampToken | 不返回（二进制） |
| ArcFileContent | signValue | 不返回（二进制） |
| ArcFileContent | certificate | 不返回 |
| ArcFileContent | storagePath | 不返回（内部路径） |
| ArcFileContent | sourceData | 不返回（大JSON） |
| SysAuditLog | dataBefore | 仅返回长度或摘要 |
| SysAuditLog | dataAfter | 仅返回长度或摘要 |
| SysAuditLog | prevLogHash | 不返回（内部校验） |
| SysAuditLog | logHash | 不返回（内部校验） |

## 扩展指南

### 为新 Entity 创建 DTO

1. 创建 Response DTO 类
2. 添加 `@Schema` 注解用于 API 文档
3. 添加 Builder 模式支持
4. 实现 `fromEntity()` 静态方法
5. 在 DtoMapper 中添加转换方法

### 示例模板

```java
// Input: Lombok、Java 标准库、Swagger、MyBatis-Plus
// Output: XxxResponse 类
// Pos: 数据传输对象

package com.nexusarchive.dto.response;

import com.nexusarchive.entity.Xxx;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Xxx 响应 DTO
 * <p>
 * 从 Xxx Entity 转换，隐藏以下敏感/大字段：
 * - sensitiveField: 敏感字段
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Xxx信息响应")
public class XxxResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "业务字段")
    private String businessField;

    /**
     * 从 Entity 转换为 DTO
     */
    public static XxxResponse fromEntity(Xxx entity) {
        return XxxResponse.builder()
                .id(entity.getId())
                .businessField(entity.getBusinessField())
                // 只包含必要字段
                .build();
    }
}
```

## 相关文件

| 文件 | 说明 |
|------|------|
| `ApiResponse.java` | 通用响应包装器 |
| `ArchiveResponse.java` | 档案响应 DTO |
| `UserResponse.java` | 用户响应 DTO |
| `AuditLogResponse.java` | 审计日志响应 DTO |
| `FileContentResponse.java` | 文件内容响应 DTO |
| `DtoMapper.java` | DTO 转换工具类 |
