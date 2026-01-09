# ERP 差异隔离框架实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 构建一个配置化的 ERP 数据映射框架，隔离不同 ERP 的数据结构差异，支持通过 YAML 配置 + Groovy 脚本进行字段映射，由 AI 自动生成配置，真实数据自动验证。

**Architecture:** 
- 统一映射接口 ErpMapper，屏蔽 ERP 差异
- YAML 声明式配置 + Groovy 脚本处理复杂转换
- 复用 AccountingSipDto 作为统一模型，不引入新概念
- 真实 API 数据自动测试验证配置正确性

**Tech Stack:** Java 17, Spring Boot, Groovy, SnakeYAML, JUnit 5, ArchUnit

---

## Phase 1: 框架基础

### Task 1.1: 创建映射配置模型

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/mapping/MappingConfig.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/mapping/FieldMapping.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/mapping/ObjectMapping.java`

**Step 1: 创建 FieldMapping.java**

```java
// Input: Lombok, Java 标准库
// Output: FieldMapping 类
// Pos: 集成模块 - ERP 映射配置

package com.nexusarchive.integration.erp.mapping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字段映射配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMapping {
    /**
     * 源字段名（简单映射时使用）
     */
    private String field;
    
    /**
     * Groovy 脚本（复杂转换时使用）
     */
    private String script;
    
    /**
     * 类型转换
     */
    private String type;
    
    /**
     * 格式化模式
     */
    private String format;
    
    /**
     * 是否为复杂脚本（多行）
     */
    public boolean isScript() {
        return script != null && !script.isBlank();
    }
}
```

**Step 2: 创建 ObjectMapping.java**

```java
// Input: Lombok, Java 标准库
// Output: ObjectMapping 类
// Pos: 集成模块 - ERP 映射配置

package com.nexusarchive.integration.erp.mapping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 对象映射配置（用于 entries、attachments 等数组）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectMapping {
    /**
     * 源数组字段名
     */
    private String source;
    
    /**
     * 数组元素的字段映射
     */
    private Map<String, FieldMapping> item;
}
```

**Step 3: 创建 MappingConfig.java**

```java
// Input: Lombok, Java 标准库
// Output: MappingConfig 类
// Pos: 集成模块 - ERP 映射配置

package com.nexusarchive.integration.erp.mapping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * ERP 映射配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingConfig {
    /**
     * ERP 系统标识
     */
    private String sourceSystem;
    
    /**
     * 目标模型
     */
    private String targetModel;
    
    /**
     * 配置版本
     */
    private String version;
    
    /**
     * 顶层字段映射（header 的字段）
     */
    private Map<String, FieldMapping> headerMappings;
    
    /**
     * 分录映射
     */
    private ObjectMapping entries;
    
    /**
     * 附件映射
     */
    private ObjectMapping attachments;
}
```

**Step 4: 运行编译验证**

```bash
cd /Users/user/nexusarchive/nexusarchive-java
mvn compile -q
```

Expected: 编译成功，无错误

**Step 5: 提交**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/mapping/
git commit -m "feat(mapping): add mapping config models"
```

---

### Task 1.2: 创建 Groovy 脚本执行引擎

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/mapping/GroovyMappingEngine.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/mapping/ScriptContext.java`
- Modify: `nexusarchive-java/pom.xml` - 添加 Groovy 依赖

**Step 1: 添加 Groovy 依赖到 pom.xml**

在 `<dependencies>` 节点添加：

```xml
<!-- Groovy for dynamic mapping scripts -->
<dependency>
    <groupId>org.apache.groovy</groupId>
    <artifactId>groovy-all</artifactId>
    <version>4.0.15</version>
    <type>pom</type>
</dependency>
```

**Step 2: 创建 ScriptContext.java**

```java
// Input: Lombok, Java 标准库
// Output: ScriptContext 类
// Pos: 集成模块 - ERP 映射脚本上下文

package com.nexusarchive.integration.erp.mapping;

import com.nexusarchive.integration.erp.dto.ErpConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Groovy 脚本执行上下文
 */
@Data
@Builder
@AllArgsConstructor
public class ScriptContext {
    /**
     * 当前 ERP 响应对象
     */
    private Object ctx;
    
    /**
     * ERP 配置
     */
    private ErpConfig config;
    
    /**
     * 工具类
     */
    private Map<String, Object> utils;
    
    /**
     * 获取绑定的变量用于 GroovyShell
     */
    public Map<String, Object> getBindings() {
        return Map.of(
            "ctx", ctx,
            "config", config,
            "utils", utils
        );
    }
}
```

**Step 3: 创建 GroovyMappingEngine.java**

```java
// Input: Groovy, Lombok, Java 标准库, SLF4J
// Output: GroovyMappingEngine 类
// Pos: 集成模块 - ERP 映射脚本引擎

package com.nexusarchive.integration.erp.mapping;

import groovy.lang.GroovyShell;
import groovy.lang.Binding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Groovy 脚本执行引擎
 * 用于执行字段映射中的动态脚本
 */
@Slf4j
@Component
public class GroovyMappingEngine {
    
    private final GroovyShell shell;
    
    public GroovyMappingEngine() {
        this.shell = new GroovyShell();
    }
    
    /**
     * 执行脚本并返回结果
     * 
     * @param script Groovy 脚本
     * @param context 执行上下文
     * @return 脚本执行结果
     */
    public Object execute(String script, ScriptContext context) {
        try {
            Binding binding = new Binding(context.getBindings());
            Object result = shell.evaluate(script, binding);
            log.debug("Script executed successfully: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Script execution failed: {}", script, e);
            throw new MappingScriptException("Script execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * 安全地执行脚本，返回默认值如果失败
     */
    public Object executeSafe(String script, ScriptContext context, Object defaultValue) {
        try {
            return execute(script, context);
        } catch (Exception e) {
            log.warn("Script failed, returning default value: {}", defaultValue);
            return defaultValue;
        }
    }
}
```

**Step 4: 创建异常类**

```java
// Input: RuntimeException, Java 标准库
// Output: MappingScriptException 类
// Pos: 集成模块 - ERP 映射异常

package com.nexusarchive.integration.erp.mapping;

public class MappingScriptException extends RuntimeException {
    
    public MappingScriptException(String message) {
        super(message);
    }
    
    public MappingScriptException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**Step 5: 编译验证**

```bash
cd /Users/user/nexusarchive/nexusarchive-java
mvn compile -q
```

**Step 6: 提交**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/mapping/ nexusarchive-java/pom.xml
git commit -m "feat(mapping): add Groovy script execution engine"
```

---

### Task 1.3: 创建配置加载器

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/mapping/MappingConfigLoader.java`
- Create: `nexusarchive-java/src/main/resources/erp-mapping/yonsuite-mapping.yml`
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/mapping/MappingConfigLoaderTest.java`

**Step 1: 创建 yonsuite-mapping.yml（示例配置）**

```yaml
# YonSuite ERP 映射配置
sourceSystem: yonsuite
targetModel: AccountingSipDto
version: 1.0.0

# 顶层字段映射
headerMappings:
  voucherNumber:
    field: displayname
  accountPeriod:
    field: period
  voucherDate:
    field: maketime
    type: date
  issuer:
    field: maker.name
  attachmentCount:
    field: attachmentCount

# 分录映射
entries:
  source: body
  item:
    lineNo:
      field: lineNo
    summary:
      field: description
    accountCode:
      field: accountCode
    accountName:
      field: accountName
    debit:
      field: debitAmount
    credit:
      field: creditAmount

# 附件映射
attachments:
  source: attachments
  item:
    attachmentId:
      field: id
    fileName:
      field: fileName
    fileSize:
      field: fileSize
    downloadUrl:
      field: url
```

**Step 2: 创建 MappingConfigLoader.java**

```java
// Input: SnakeYAML, Spring, Lombok, SLF4J
// Output: MappingConfigLoader 类
// Pos: 集成模块 - ERP 映射配置加载器

package com.nexusarchive.integration.erp.mapping;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;

/**
 * 映射配置加载器
 * 从 classpath:erp-mapping/ 目录加载 YAML 配置
 */
@Slf4j
@Component
public class MappingConfigLoader {
    
    private static final String MAPPING_BASE_PATH = "erp-mapping/";
    
    private final Yaml yaml;
    
    public MappingConfigLoader() {
        this.yaml = new Yaml(new Constructor(MappingConfig.class));
    }
    
    /**
     * 加载指定 ERP 的映射配置
     * 
     * @param sourceSystem ERP 标识（如 yonsuite, kingdee）
     * @return 映射配置
     * @throws IOException 配置文件不存在或格式错误
     */
    public MappingConfig loadMapping(String sourceSystem) throws IOException {
        String configPath = MAPPING_BASE_PATH + sourceSystem + "-mapping.yml";
        log.info("Loading mapping config: {}", configPath);
        
        ClassPathResource resource = new ClassPathResource(configPath);
        
        if (!resource.exists()) {
            throw new MappingConfigNotFoundException(
                "Mapping config not found for system: " + sourceSystem + 
                ", expected path: " + configPath
            );
        }
        
        try (InputStream inputStream = resource.getInputStream()) {
            MappingConfig config = yaml.load(inputStream);
            log.info("Loaded mapping config for {}, version: {}", 
                sourceSystem, config.getVersion());
            return config;
        } catch (Exception e) {
            log.error("Failed to parse mapping config: {}", configPath, e);
            throw new MappingConfigParseException(
                "Failed to parse mapping config: " + e.getMessage(), e
            );
        }
    }
    
    /**
     * 检查配置是否存在
     */
    public boolean mappingExists(String sourceSystem) {
        String configPath = MAPPING_BASE_PATH + sourceSystem + "-mapping.yml";
        return new ClassPathResource(configPath).exists();
    }
}
```

**Step 3: 创建异常类**

```java
// MappingConfigNotFoundException.java
package com.nexusarchive.integration.erp.mapping;

public class MappingConfigNotFoundException extends RuntimeException {
    public MappingConfigNotFoundException(String message) {
        super(message);
    }
}

// MappingConfigParseException.java
package com.nexusarchive.integration.erp.mapping;

public class MappingConfigParseException extends RuntimeException {
    public MappingConfigParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**Step 4: 编译并添加 SnakeYAML 依赖**

检查 pom.xml 是否已有 SnakeYAML，如果没有添加：

```xml
<dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
</dependency>
```

**Step 5: 提交**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/mapping/ nexusarchive-java/src/main/resources/erp-mapping/
git commit -m "feat(mapping): add mapping config loader"
```

---

### Task 1.4: 创建统一映射接口 ErpMapper

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/mapping/ErpMapper.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/mapping/DefaultErpMapper.java`
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/mapping/ErpMapperTest.java`

**Step 1: 创建 ErpMapper 接口**

```java
// Input: Spring, Java 标准库
// Output: ErpMapper 接口
// Pos: 集成模块 - ERP 统一映射接口

package com.nexusarchive.integration.erp.mapping;

import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.integration.erp.dto.ErpConfig;

/**
 * ERP 统一映射接口
 * 屏蔽不同 ERP 的数据结构差异
 */
public interface ErpMapper {
    
    /**
     * 将 ERP 响应转换为 AccountingSipDto
     * 
     * @param erpResponse ERP 原始响应对象
     * @param sourceSystem ERP 标识
     * @param config ERP 配置
     * @return 标准化的 SIP DTO
     */
    AccountingSipDto mapToSipDto(Object erpResponse, String sourceSystem, ErpConfig config);
    
    /**
     * 批量映射
     */
    default java.util.List<AccountingSipDto> mapToSipDto(
            java.util.List<?> erpResponses, 
            String sourceSystem, 
            ErpConfig config) {
        return erpResponses.stream()
            .map(r -> mapToSipDto(r, sourceSystem, config))
            .toList();
    }
}
```

**Step 2: 创建 DefaultErpMapper 实现**

```java
// Input: Spring, Lombok, SLF4J
// Output: DefaultErpMapper 类
// Pos: 集成模块 - ERP 默认映射实现

package com.nexusarchive.integration.erp.mapping;

import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.dto.sip.VoucherEntryDto;
import com.nexusarchive.dto.sip.AttachmentDto;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 默认 ERP 映射器
 * 基于配置文件进行字段映射
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultErpMapper implements ErpMapper {
    
    private final MappingConfigLoader configLoader;
    private final GroovyMappingEngine scriptEngine;
    
    @Override
    public AccountingSipDto mapToSipDto(Object erpResponse, String sourceSystem, ErpConfig config) {
        log.debug("Mapping ERP response to SIP DTO, sourceSystem: {}", sourceSystem);
        
        MappingConfig mappingConfig = configLoader.loadMapping(sourceSystem);
        
        AccountingSipDto dto = new AccountingSipDto();
        dto.setRequestId(java.util.UUID.randomUUID().toString());
        dto.setSourceSystem(sourceSystem);
        
        // 映射 header
        VoucherHeadDto header = mapHeader(erpResponse, mappingConfig, config);
        dto.setHeader(header);
        
        // 映射 entries
        List<VoucherEntryDto> entries = mapEntries(erpResponse, mappingConfig, config);
        dto.setEntries(entries);
        
        // 映射 attachments
        List<AttachmentDto> attachments = mapAttachments(erpResponse, mappingConfig, config);
        dto.setAttachments(attachments);
        
        return dto;
    }
    
    private VoucherHeadDto mapHeader(Object response, MappingConfig config, ErpConfig erpConfig) {
        VoucherHeadDto header = new VoucherHeadDto();
        ScriptContext scriptContext = ScriptContext.builder()
            .ctx(response)
            .config(erpConfig)
            .build();
        
        for (Map.Entry<String, FieldMapping> entry : config.getHeaderMappings().entrySet()) {
            String targetField = entry.getKey();
            FieldMapping mapping = entry.getValue();
            
            Object value = extractValue(response, mapping, scriptContext);
            setFieldValue(header, targetField, value);
        }
        
        return header;
    }
    
    private List<VoucherEntryDto> mapEntries(Object response, MappingConfig config, ErpConfig erpConfig) {
        List<VoucherEntryDto> entries = new ArrayList<>();
        ObjectMapping entryMapping = config.getEntries();
        
        if (entryMapping == null || entryMapping.getSource() == null) {
            return entries;
        }
        
        // 提取源数组
        List<?> sourceArray = extractArray(response, entryMapping.getSource());
        if (sourceArray == null) {
            return entries;
        }
        
        ScriptContext scriptContext = ScriptContext.builder()
            .config(erpConfig)
            .build();
        
        for (Object item : sourceArray) {
            scriptContext.setCtx(item);
            VoucherEntryDto entry = mapObject(item, entryMapping.getItem(), scriptContext, VoucherEntryDto.class);
            entries.add(entry);
        }
        
        return entries;
    }
    
    private List<AttachmentDto> mapAttachments(Object response, MappingConfig config, ErpConfig erpConfig) {
        List<AttachmentDto> attachments = new ArrayList<>();
        ObjectMapping attachmentMapping = config.getAttachments();
        
        if (attachmentMapping == null || attachmentMapping.getSource() == null) {
            return attachments;
        }
        
        List<?> sourceArray = extractArray(response, attachmentMapping.getSource());
        if (sourceArray == null) {
            return attachments;
        }
        
        ScriptContext scriptContext = ScriptContext.builder()
            .config(erpConfig)
            .build();
        
        for (Object item : sourceArray) {
            scriptContext.setCtx(item);
            AttachmentDto attachment = mapObject(item, attachmentMapping.getItem(), scriptContext, AttachmentDto.class);
            attachments.add(attachment);
        }
        
        return attachments;
    }
    
    private <T> T mapObject(Object source, Map<String, FieldMapping> mappings, 
                           ScriptContext context, Class<T> targetClass) {
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            
            for (Map.Entry<String, FieldMapping> entry : mappings.entrySet()) {
                String targetField = entry.getKey();
                FieldMapping mapping = entry.getValue();
                
                Object value = extractValue(source, mapping, context);
                setFieldValue(target, targetField, value);
            }
            
            return target;
        } catch (Exception e) {
            log.error("Failed to map object to {}", targetClass.getSimpleName(), e);
            throw new MappingException("Failed to map object: " + e.getMessage(), e);
        }
    }
    
    private Object extractValue(Object source, FieldMapping mapping, ScriptContext context) {
        if (mapping.isScript()) {
            return scriptEngine.execute(mapping.getScript(), context);
        }
        
        if (mapping.getField() != null) {
            return getFieldValue(source, mapping.getField());
        }
        
        return null;
    }
    
    private Object getFieldValue(Object obj, String fieldPath) {
        try {
            String[] parts = fieldPath.split("\\.");
            Object current = obj;
            
            for (String part : parts) {
                if (current == null) {
                    return null;
                }
                
                // 尝试 getter 方法
                String methodName = "get" + Character.toUpperCase(part.charAt(0)) + part.substring(1);
                
                try {
                    java.lang.reflect.Method method = current.getClass().getMethod(methodName);
                    current = method.invoke(current);
                } catch (NoSuchMethodException e) {
                    // 尝试直接访问字段
                    Field field = current.getClass().getDeclaredField(part);
                    field.setAccessible(true);
                    current = field.get(current);
                }
            }
            
            return current;
        } catch (Exception e) {
            log.warn("Failed to get field value: {}", fieldPath);
            return null;
        }
    }
    
    private void setFieldValue(Object target, String fieldPath, Object value) {
        if (value == null) {
            return;
        }
        
        try {
            String[] parts = fieldPath.split("\\.");
            if (parts.length == 1) {
                // 直接字段
                String methodName = "set" + Character.toUpperCase(parts[0].charAt(0)) + parts[0].substring(1);
                
                for (java.lang.reflect.Method method : target.getClass().getMethods()) {
                    if (method.getName().equals(methodName) && 
                        method.getParameterCount() == 1) {
                        method.invoke(target, convertValue(value, method.getParameterTypes()[0]));
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to set field value: {} = {}", fieldPath, value);
        }
    }
    
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }
        
        if (targetType == String.class) {
            return value.toString();
        }
        
        if (targetType == LocalDate.class && value instanceof String) {
            return LocalDate.parse((String) value);
        }
        
        return value;
    }
    
    private List<?> extractArray(Object obj, String fieldPath) {
        Object value = getFieldValue(obj, fieldPath);
        if (value instanceof List) {
            return (List<?>) value;
        }
        return null;
    }
}
```

**Step 3: 创建异常类**

```java
// MappingException.java
package com.nexusarchive.integration.erp.mapping;

public class MappingException extends RuntimeException {
    public MappingException(String message) {
        super(message);
    }
    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**Step 4: 编译验证**

```bash
mvn compile -q
```

**Step 5: 提交**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/mapping/
git commit -m "feat(mapping): add unified ErpMapper interface and implementation"
```

---

### Task 1.5: 添加单元测试

**Files:**
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/mapping/GroovyMappingEngineTest.java`
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/mapping/MappingConfigLoaderTest.java`

**Step 1: 创建 GroovyMappingEngineTest.java**

```java
// Input: JUnit 5, Spring Test
// Output: GroovyMappingEngineTest 类
// Pos: 测试 - Groovy 脚本引擎测试

package com.nexusarchive.integration.erp.mapping;

import com.nexusarchive.integration.erp.dto.ErpConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GroovyMappingEngineTest {
    
    @Autowired
    private GroovyMappingEngine engine;
    
    @Test
    void testSimpleScript() {
        ErpConfig config = new ErpConfig();
        config.setBaseUrl("http://test");
        
        ScriptContext context = ScriptContext.builder()
            .ctx(Map.of("name", "test", "value", 100))
            .config(config)
            .build();
        
        Object result = engine.execute("return ctx.name + '-' + ctx.value", context);
        
        assertThat(result).isEqualTo("test-100");
    }
    
    @Test
    void testScriptWithDefault() {
        ScriptContext context = ScriptContext.builder()
            .ctx(Map.of("field", "value"))
            .build();
        
        Object result = engine.executeSafe(
            "return ctx.nonExistentField", 
            context, 
            "DEFAULT"
        );
        
        assertThat(result).isEqualTo("DEFAULT");
    }
}
```

**Step 2: 创建 MappingConfigLoaderTest.java**

```java
// Input: JUnit 5, Spring Test
// Output: MappingConfigLoaderTest 类
// Pos: 测试 - 配置加载器测试

package com.nexusarchive.integration.erp.mapping;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class MappingConfigLoaderTest {
    
    @Autowired
    private MappingConfigLoader loader;
    
    @Test
    void loadYonSuiteMapping() {
        MappingConfig config = loader.loadMapping("yonsuite");
        
        assertThat(config.getSourceSystem()).isEqualTo("yonsuite");
        assertThat(config.getTargetModel()).isEqualTo("AccountingSipDto");
        assertThat(config.getHeaderMappings()).isNotEmpty();
    }
    
    @Test
    void loadNonExistentMapping() {
        assertThatThrownBy(() -> loader.loadMapping("nonexistent"))
            .isInstanceOf(MappingConfigNotFoundException.class);
    }
    
    @Test
    void checkMappingExists() {
        assertThat(loader.mappingExists("yonsuite")).isTrue();
        assertThat(loader.mappingExists("nonexistent")).isFalse();
    }
}
```

**Step 3: 运行测试**

```bash
mvn test -Dtest=GroovyMappingEngineTest,MappingConfigLoaderTest
```

Expected: 所有测试通过

**Step 4: 提交**

```bash
git add nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/mapping/
git commit -m "test(mapping): add unit tests for mapping framework"
```

---

## Phase 2: YonSuite 迁移

### Task 2.1: 提取 YonSuite 转换逻辑为配置

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/client/YonSuiteVoucherClient.java`
- Modify: `nexusarchive-java/src/main/resources/erp-mapping/yonsuite-mapping.yml`
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/mapping/YonSuiteMappingMigrationTest.java`

**Step 1: 分析现有转换逻辑**

查看 `YonSuiteVoucherClient.convertToVoucherDTO()` 方法，记录所有字段映射关系。

**Step 2: 更新 yonsuite-mapping.yml**

根据分析结果，完善配置文件：

```yaml
# YonSuite ERP 映射配置
sourceSystem: yonsuite
targetModel: AccountingSipDto
version: 1.0.0

# 顶层字段映射
headerMappings:
  voucherNumber:
    field: displayname
  accountPeriod:
    field: period
  voucherDate:
    field: maketime
    type: date
    format: "yyyy-MM-dd"
  issuer:
    field: maker.name
  attachmentCount:
    field: attachmentCount
  debitTotal:
    field: totalDebitOrg
  creditTotal:
    field: totalCreditOrg
  accbookCode:
    script: "groovy:return config.accbookCode"

# 分录映射
entries:
  source: body
  item:
    lineNo:
      field: lineNo
    summary:
      field: description
    accountCode:
      field: accountCode
    accountName:
      field: accountName
    debit:
      field: debitAmount
    credit:
      field: creditAmount

# 附件映射
attachments:
  source: attachments
  item:
    attachmentId:
      field: id
    fileName:
      field: fileName
    fileSize:
      field: fileSize
    downloadUrl:
      field: url
```

**Step 3: 创建迁移测试**

```java
// Input: JUnit 5, Spring Test
// Output: YonSuiteMappingMigrationTest 类
// Pos: 测试 - YonSuite 映射迁移验证

package com.nexusarchive.integration.erp.mapping;

import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherListResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 YonSuite 映射配置与原有逻辑结果一致
 */
@SpringBootTest
class YonSuiteMappingMigrationTest {
    
    @Autowired
    private ErpMapper erpMapper;
    
    @Test
    void yonSuiteMappingShouldMatchOriginalLogic() {
        // 准备测试数据
        YonVoucherListResponse.VoucherHeader originalHeader = createTestHeader();
        
        ErpConfig config = new ErpConfig();
        config.setAccbookCode("001");
        
        // 执行映射
        AccountingSipDto result = erpMapper.mapToSipDto(originalHeader, "yonsuite", config);
        
        // 验证结果
        assertThat(result.getHeader().getVoucherNumber()).isEqualTo("记-001");
        assertThat(result.getHeader().getAccountPeriod()).isEqualTo("2024-01");
        assertThat(result.getEntries()).hasSize(2);
    }
    
    private YonVoucherListResponse.VoucherHeader createTestHeader() {
        YonVoucherListResponse.VoucherHeader header = new YonVoucherListResponse.VoucherHeader();
        header.setDisplayname("记-001");
        header.setPeriod("2024-01");
        // ... 设置其他字段
        return header;
    }
}
```

**Step 4: 提交**

```bash
git add nexusarchive-java/src/main/resources/erp-mapping/ nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/mapping/
git commit -m "feat(mapping): extract YonSuite conversion to config"
```

---

### Task 2.2: 移除硬编码转换逻辑

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/client/YonSuiteVoucherClient.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/YonSuiteErpAdapter.java`

**Step 1: 修改 YonSuiteVoucherClient**

移除 `convertToVoucherDTO` 相关方法，只保留原始 API 调用：

```java
// 删除或注释掉：
// - convertToVoucherDTO()
// - convertToVoucherDTOs()
// - parseVoucherDate()
// - deriveVoucherNo()
// - deriveVoucherWord()
// - deriveSummary()

// 保留：
// - syncVouchers() 但返回原始 YonVoucherListResponse
// - getVoucherDetail() 返回原始 YonVoucherDetailResponse
// - getAttachments()
// - testConnection()
```

**Step 2: 修改 YonSuiteErpAdapter**

注入 `ErpMapper`，在返回前进行映射：

```java
@RequiredArgsConstructor
public class YonSuiteErpAdapter implements ErpAdapter {
    
    private final YonSuiteAuthClient authClient;
    private final YonSuiteVoucherClient voucherClient;
    // ... 其他客户端
    
    // 新增：注入映射器
    private final ErpMapper erpMapper;
    
    @Override
    public List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        // 1. 调用原始 API
        List<YonVoucherListResponse> rawResponses = voucherClient.fetchRawVouchers(...);
        
        // 2. 使用映射器转换
        List<AccountingSipDto> sipDtos = erpMapper.mapToSipDto(rawResponses, "yonsuite", config);
        
        // 3. 转换为 VoucherDTO（向后兼容）
        return sipDtos.stream()
            .map(this::toVoucherDTO)
            .toList();
    }
}
```

**Step 3: 编译并测试**

```bash
mvn test -Dtest=YonSuiteErpAdapterTest
```

**Step 4: 提交**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/
git commit -m "refactor(yonsuite): remove hardcoded conversion logic"
```

---

## Phase 3: 金蝶实现

### Task 3.1: 实现金蝶原始数据获取

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/KingdeeAdapter.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/client/KingdeeVoucherClient.java`

**Step 1: 创建 KingdeeVoucherClient.java**

```java
// Input: Spring, Hutool, Lombok, SLF4J
// Output: KingdeeVoucherClient 类
// Pos: 集成模块 - 金蝶 API 客户端

package com.nexusarchive.integration.erp.adapter.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 金蝶云星空凭证客户端
 * 负责调用金蝶 API 获取原始数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KingdeeVoucherClient {
    
    private static final String QUERY_PATH = 
        "/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";
    
    /**
     * 获取原始凭证数据（不进行转换）
     * 
     * @param accessToken 访问令牌
     * @param baseUrl 基础 URL
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 原始 JSON 对象列表
     */
    public List<JSONObject> fetchRawVouchers(String accessToken, String baseUrl,
                                              LocalDate startDate, LocalDate endDate) {
        log.info("Kingdee fetch raw vouchers: {} ~ {}", startDate, endDate);
        
        try {
            String url = baseUrl + QUERY_PATH;
            
            JSONObject queryParams = buildQueryRequest(startDate, endDate);
            
            String response = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .header("Apiv-Acctoken", accessToken)
                .body(queryParams.toString())
                .timeout(30000)
                .execute()
                .body();
            
            return parseResponse(response);
            
        } catch (Exception e) {
            log.error("Kingdee fetch raw vouchers error", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取单个凭证详情（原始数据）
     */
    public JSONObject fetchRawVoucherDetail(String accessToken, String baseUrl, String voucherId) {
        // 实现详情查询
        return null;
    }
    
    /**
     * 获取凭证附件列表（原始数据）
     */
    public List<JSONObject> fetchRawAttachments(String accessToken, String baseUrl, String voucherId) {
        // 实现附件查询
        return Collections.emptyList();
    }
    
    private JSONObject buildQueryRequest(LocalDate startDate, LocalDate endDate) {
        JSONObject request = JSONUtil.createObj()
            .set("FormId", "GL_VOUCHER")
            .set("FieldKeys", "FVoucherID,FDate,FNumber,FAttachments,FEXPLANATION")
            .set("FilterString", String.format(
                "FDate>='%s' AND FDate<='%s'",
                startDate.format(DateTimeFormatter.ISO_DATE),
                endDate.format(DateTimeFormatter.ISO_DATE)
            ))
            .set("OrderString", "FDate ASC")
            .set("TopRowCount", 100)
            .set("StartRow", 0);
        
        return request;
    }
    
    private List<JSONObject> parseResponse(String response) {
        List<JSONObject> results = new ArrayList<>();
        
        JSONObject json = JSONUtil.parseObj(response);
        var data = json.getJSONObject("Result");
        
        if (data != null && data.containsKey("ResponseStatus")) {
            var status = data.getJSONObject("ResponseStatus");
            if (status.getInt("Code") == 1) {
                var records = data.getJSONArray("Result");
                if (records != null) {
                    for (int i = 0; i < records.size(); i++) {
                        results.add(records.getJSONObject(i));
                    }
                }
            }
        }
        
        return results;
    }
}
```

**Step 2: 修改 KingdeeAdapter**

注入 `KingdeeVoucherClient` 和 `ErpMapper`：

```java
@ErpAdapterAnnotation(...)
@Service("kingdee")
@RequiredArgsConstructor
public class KingdeeAdapter implements ErpAdapter {
    
    private final KingdeeVoucherClient voucherClient;
    private final ErpMapper erpMapper;
    
    @Override
    public List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        String accessToken = authenticate(config);
        
        // 1. 获取原始数据
        List<JSONObject> rawResponses = voucherClient.fetchRawVouchers(
            accessToken, config.getBaseUrl(), startDate, endDate
        );
        
        // 2. 映射为标准 DTO
        List<AccountingSipDto> sipDtos = erpMapper.mapToSipDto(rawResponses, "kingdee", config);
        
        // 3. 转换为 VoucherDTO（向后兼容）
        return sipDtos.stream()
            .map(this::toVoucherDTO)
            .toList();
    }
    
    private VoucherDTO toVoucherDTO(AccountingSipDto sip) {
        // 转换逻辑
        return null;
    }
}
```

**Step 3: 提交**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/
git commit -m "feat(kingdee): add raw data fetcher"
```

---

### Task 3.2: 创建金蝶映射配置

**Files:**
- Create: `nexusarchive-java/src/main/resources/erp-mapping/kingdee-mapping.yml`
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/mapping/KingdeeMappingTest.java`

**Step 1: 创建 kingdee-mapping.yml**

```yaml
# 金蝶云星空 ERP 映射配置
sourceSystem: kingdee
targetModel: AccountingSipDto
version: 1.0.0

# 顶层字段映射
headerMappings:
  voucherNumber:
    field: FNumber
  accountPeriod:
    script: |
      groovy:
        def year = ctx.FDate?.substring(0, 4)
        def month = ctx.FDate?.substring(5, 7)
        return year + '-' + month
  voucherDate:
    field: FDate
    type: date
    format: "yyyy-MM-dd"
  issuer:
    field: FMaker
  attachmentCount:
    field: FAttachments
    script: "groovy:return ctx.FAttachments?.size() ?: 0"
  debitTotal:
    field: FTotalDebit
  creditTotal:
    field: FTotalCredit

# 分录映射
entries:
  source: FDetails
  item:
    lineNo:
      field: FLineNo
    summary:
      field: FExplanation
    accountCode:
      field: FAccountCode
    accountName:
      field: FAccountName
    debit:
      field: FDebitAmount
      script: "groovy:return ctx.FDebitAmount ?: 0"
    credit:
      field: FCreditAmount
      script: "groovy:return ctx.FCreditAmount ?: 0"

# 附件映射
attachments:
  source: FAttachments
  item:
    attachmentId:
      field: FID
    fileName:
      field: FFileName
    fileSize:
      field: FFileSize
    downloadUrl:
      script: |
      groovy:
        def baseUrl = config.getBaseUrl()
        return baseUrl + '/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc?fileId=' + ctx.FID
```

**Step 2: 创建集成测试**

```java
// Input: JUnit 5, Spring Test
// Output: KingdeeMappingTest 类
// Pos: 测试 - 金蝶映射集成测试

package com.nexusarchive.integration.erp.mapping;

import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import cn.hutool.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 金蝶映射集成测试
 * 使用真实 API 数据验证配置
 */
@SpringBootTest
class KingdeeMappingTest {
    
    @Autowired
    private ErpMapper erpMapper;
    
    @Autowired
    private MappingConfigLoader configLoader;
    
    @Test
    void kingdeeConfigShouldLoad() {
        MappingConfig config = configLoader.loadMapping("kingdee");
        
        assertThat(config.getSourceSystem()).isEqualTo("kingdee");
        assertThat(config.getHeaderMappings()).isNotEmpty();
    }
    
    @Test
    void mapKingdeeRawResponse() {
        // 模拟金蝶响应
        JSONObject mockResponse = createMockKingdeeResponse();
        
        ErpConfig config = new ErpConfig();
        config.setBaseUrl("http://test.kingdee.com");
        
        AccountingSipDto result = erpMapper.mapToSipDto(mockResponse, "kingdee", config);
        
        // 验证映射结果
        assertThat(result.getHeader().getVoucherNumber()).isEqualTo("KD-001");
        assertThat(result.getHeader().getAccountPeriod()).isEqualTo("2024-01");
    }
    
    private JSONObject createMockKingdeeResponse() {
        JSONObject response = new JSONObject();
        response.set("FNumber", "KD-001");
        response.set("FDate", "2024-01-15");
        response.set("FAttachments", new Object[0]);
        
        JSONObject detail = new JSONObject();
        detail.set("FLineNo", 1);
        detail.set("FExplanation", "测试摘要");
        detail.set("FAccountCode", "1001");
        detail.set("FDebitAmount", 1000.00);
        detail.set("FCreditAmount", 0);
        
        response.set("FDetails", new Object[]{detail});
        
        return response;
    }
}
```

**Step 3: 运行测试**

```bash
mvn test -Dtest=KingdeeMappingTest
```

**Step 4: 提交**

```bash
git add nexusarchive-java/src/main/resources/erp-mapping/ nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/mapping/
git commit -m "feat(kingdee): add mapping config and integration test"
```

---

## 验收标准

### 功能验收

- [ ] 所有 Phase 1 任务完成，框架基础运行正常
- [ ] YonSuite 迁移完成，与原有逻辑结果一致
- [ ] 金蝶实现完成，能正确映射数据
- [ ] 所有单元测试和集成测试通过

### 架构验收

- [ ] ArchUnit 测试验证：新模块符合分层架构
- [ ] 无循环依赖
- [ ] 复杂度规则通过

### 文档验收

- [ ] 映射配置格式文档化
- [ ] 新增 ERP 接入流程文档更新

---

## 附录：相关文档

- [设计文档](2026-01-08-erp-mapping-framework.md)
- [模块清单](../architecture/module-manifest.md)
- [架构规则](../architecture/module-boundaries.md)
