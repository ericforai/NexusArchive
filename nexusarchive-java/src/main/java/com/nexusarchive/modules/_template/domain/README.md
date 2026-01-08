一旦我所属的文件夹有所变化，请更新我。
本目录存放模块的领域层（Entity、Value Object、Status、Repository 接口）。

## 职责

- 核心业务逻辑
- 领域模型（聚合根、实体、值对象）
- 业务规则
- 仓储接口定义

## 依赖规则

- ✅ 可依赖: JDK、Lombok、Common 工具类
- ❌ 禁止依赖: Spring、MyBatis、infra/、app/、api/

## 文件命名

- 聚合根: `{ModuleName}.java`
- 值对象: `{ModuleName}Id.java`、`{ModuleName}Status.java`
- 仓储接口: `{ModuleName}Repository.java`

## 模板

```java
// Input: 领域模型、业务规则
// Output: {ModuleName} 聚合根
// Pos: {module}/domain
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.{module}.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * {ModuleName} 聚合根
 * <p>
 * 核心业务逻辑封装
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class {ModuleName} {

    private {ModuleName}Id id;

    private String name;

    private {ModuleName}Status status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 业务规则：{ModuleName} 状态变更
     */
    public void transitionTo({ModuleName}Status newStatus) {
        // 业务规则验证
        if (!canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                "Cannot transition from " + this.status + " to " + newStatus
            );
        }
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    private boolean canTransitionTo({ModuleName}Status newStatus) {
        // 状态机规则
        return true;
    }
}
```

## 仓储接口模板

```java
// Input: 仓储接口定义
// Output: {ModuleName}Repository 接口
// Pos: {module}/domain
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.{module}.domain;

import java.util.List;
import java.util.Optional;

/**
 * {ModuleName} 仓储接口
 * <p>
 * 由 infra 层实现，domain 层定义
 * </p>
 */
public interface {ModuleName}Repository {

    {ModuleName} save({ModuleName} entity);

    Optional<{ModuleName}> findById({ModuleName}Id id);

    List<{ModuleName}> findAll();

    void deleteById({ModuleName}Id id);
}
```
