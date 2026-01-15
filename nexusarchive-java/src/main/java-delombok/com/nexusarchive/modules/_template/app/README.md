一旦我所属的文件夹有所变化，请更新我。
本目录存放模块的应用层（Facade + 用例编排）。

## 职责

- 编排业务流程
- 协调领域对象和基础设施
- 实现 Facade 接口
- 事务管理

## 依赖规则

- ✅ 可依赖: `domain/`、`infra/`、`api.dto/`
- ❌ 禁止依赖: 其他模块的 infra 层

## 文件命名

- Facade 接口: `{ModuleName}Facade.java`
- Facade 实现: `{ModuleName}FacadeImpl.java` (可选，或在 app/impl/)
- 应用服务: `{ModuleName}ApplicationService.java`

## 模板

```java
// Input: 领域对象、仓储接口、DTO
// Output: {ModuleName}Facade 接口
// Pos: {module}/app
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.{module}.app;

import com.nexusarchive.modules.{module}.api.dto.{ModuleName}Request;
import com.nexusarchive.modules.{module}.api.dto.{ModuleName}Response;
import com.nexusarchive.modules.{module}.domain.{ModuleName};
import java.util.List;

/**
 * {ModuleName} 模块 Facade
 * <p>
 * 模块对外暴露的唯一入口，隐藏内部实现细节
 * </p>
 */
public interface {ModuleName}Facade {

    {ModuleName}Response create({ModuleName}Request request);

    {ModuleName}Response getById(String id);

    List<{ModuleName}Response> list();

    void update(String id, {ModuleName}Request request);

    void delete(String id);
}
```
