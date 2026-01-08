一旦我所属的文件夹有所变化，请更新我。
本目录存放模块的对外接口层（Controller + DTO）。

## 职责

- 定义 REST API 端点
- 处理 HTTP 请求/响应
- 参数验证
- 调用应用层 Facade

## 依赖规则

- ✅ 可依赖: `app/`、`api.dto/`
- ❌ 禁止依赖: `domain/`、`infra/`、其他模块的内部实现

## 文件命名

- Controller: `{ModuleName}Controller.java`
- DTO: `{ModuleName}Request.java`、`{ModuleName}Response.java`、`{ModuleName}Dto.java`

## 模板

```java
// Input: Spring Framework、Jakarta EE、Lombok、模块 Facade
// Output: {ModuleName}Controller 类
// Pos: {module}/api
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.{module}.api;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.modules.{module}.app.{ModuleName}Facade;
import com.nexusarchive.modules.{module}.api.dto.{ModuleName}Request;
import com.nexusarchive.modules.{module}.api.dto.{ModuleName}Response;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/{module}")
@RequiredArgsConstructor
@Tag(name = "{ModuleName} API")
public class {ModuleName}Controller {

    private final {ModuleName}Facade facade;

    @PostMapping
    public Result<{ModuleName}Response> create(@Valid @RequestBody {ModuleName}Request request) {
        return Result.success(facade.create(request));
    }

    @GetMapping("/{id}")
    public Result<{ModuleName}Response> getById(@PathVariable String id) {
        return Result.success(facade.getById(id));
    }
}
```
