一旦我所属的文件夹有所变化，请更新我。
本目录存放模块的基础设施层（Mapper、Repository 实现、外部服务适配器）。

## 职责

- 数据访问（MyBatis Mapper）
- 仓储接口实现
- 外部服务调用
- 技术细节封装

## 依赖规则

- ✅ 可依赖: domain/、Spring、MyBatis、其他 infra/
- ❌ 禁止依赖: api/、app/

## 文件命名

- Mapper: `{ModuleName}Mapper.java`
- 仓储实现: `{ModuleName}RepositoryImpl.java`

## 模板

```java
// Input: MyBatis-Plus、领域对象
// Output: {ModuleName}Mapper
// Pos: {module}/infra
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.{module}.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.modules.{module}.domain.{ModuleName};
import org.apache.ibatis.annotations.Mapper;

/**
 * {ModuleName} MyBatis Mapper
 * <p>
 * 负责数据库操作
 * </p>
 */
@Mapper
public interface {ModuleName}Mapper extends BaseMapper<{ModuleName}> {
    // 自定义 SQL 方法
}
```

## 仓储实现模板

```java
// Input: 仓储接口、Mapper
// Output: {ModuleName}RepositoryImpl
// Pos: {module}/infra
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.{module}.infra;

import com.nexusarchive.modules.{module}.domain.{ModuleName};
import com.nexusarchive.modules.{module}.domain.{ModuleName}Id;
import com.nexusarchive.modules.{module}.domain.{ModuleName}Repository;
import com.nexusarchive.modules.{module}.infra.mapper.{ModuleName}Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * {ModuleName} 仓储实现
 * <p>
 * 实现 domain 层定义的仓储接口
 * </p>
 */
@Repository
@RequiredArgsConstructor
public class {ModuleName}RepositoryImpl implements {ModuleName}Repository {

    private final {ModuleName}Mapper mapper;

    @Override
    public {ModuleName} save({ModuleName} entity) {
        if (entity.getId() == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return entity;
    }

    @Override
    public Optional<{ModuleName}> findById({ModuleName}Id id) {
        return Optional.ofNullable(mapper.selectById(id.value()));
    }

    @Override
    public List<{ModuleName}> findAll() {
        return mapper.selectList(null);
    }

    @Override
    public void deleteById({ModuleName}Id id) {
        mapper.deleteById(id.value());
    }
}
```
