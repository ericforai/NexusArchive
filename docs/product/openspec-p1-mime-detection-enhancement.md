# 电子会计档案系统 - MIME 检测增强 OpenSpec

> **版本**: v1.0  
> **日期**: 2025-01  
> **对齐基准**: [开发路线图 v1.0](../planning/development_roadmap_v1.0.md)  
> **优先级**: **P1（重要但非阻塞 - 短期优化）**  
> **当前状态**: 待开发

---

## 📊 功能概览

| 功能模块 | 路线图章节 | 优先级 | 预计工作量 | 依赖关系 |
|---------|-----------|--------|-----------|---------|
| MIME 检测增强 | 阶段二：四性检测引擎 | **P1** | 1 周 | 无 |

---

## 🎯 业务目标

**用户故事**: 作为系统管理员，我需要确保系统能够准确识别上传文件的真实类型，防止恶意文件伪装成合法格式，确保四性检测的准确性。

**业务价值**:
- 提高文件类型检测的准确性和覆盖率
- 增强系统安全性（防止恶意文件攻击）
- 确保四性检测中"可用性"检测的可靠性
- 符合合规要求（DA/T 94-2022 要求文件类型验证）

**问题背景**:
- 当前 `FileMagicValidator` 使用 Magic Number 检测，但覆盖度可能不足
- `pom.xml` 中 Apache Tika 依赖被注释，未启用
- 需要评估当前实现是否满足需求，或需要启用 Tika 作为补充

---

## 📋 功能范围

### 1. 当前状态评估

#### 1.1 现有实现分析

**当前实现**: `FileMagicValidator`
- 位置: `nexusarchive-java/src/main/java/com/nexusarchive/util/FileMagicValidator.java`
- 检测方式: Magic Number（文件头部字节序列）
- 支持的文件类型: PDF、OFD、JPEG、PNG、GIF、TIFF、XML、ZIP、DOC、DOCX、XLS、XLSX

**Apache Tika 依赖状态**:
- 位置: `nexusarchive-java/pom.xml` (line 221-226)
- 状态: 已添加但可能未使用
- 版本: tika-core 2.9.1

#### 1.2 评估标准

**需要评估的指标**:
1. **覆盖率**: 当前支持的文件类型是否覆盖所有业务场景需求
2. **准确性**: Magic Number 检测是否能准确识别文件类型
3. **性能**: 检测速度是否满足性能要求
4. **维护性**: Magic Number 维护成本 vs Tika 库维护成本

**测试场景**:
- 常见文件类型：PDF、OFD、Office 文档、图片格式
- 边界情况：文件扩展名与内容不符、损坏的文件、空文件
- 恶意场景：伪装文件（.exe 伪装成 .pdf）

### 2. 决策方案

#### 2.1 方案 A：启用 Apache Tika（推荐）

**优点**:
- 支持 1000+ 文件类型
- 持续维护，更新及时
- 提供丰富的元数据提取能力
- 社区成熟，文档完善

**缺点**:
- 增加依赖大小（~5MB）
- 可能影响启动速度（首次加载）
- 需要处理 Tika 的异常情况

**实现要求**:
- 取消 `pom.xml` 中 Tika 依赖的注释（如已存在）
- 集成 Tika 到 `FileMagicValidator` 或创建新的 `TikaMimeDetector`
- 优先使用 Magic Number 检测（快速），Tika 作为兜底（准确）
- 添加性能监控，确保不影响上传性能

#### 2.2 方案 B：强化 FileMagicValidator

**优点**:
- 无额外依赖
- 性能最优（纯字节比较）
- 代码完全可控

**缺点**:
- 需要手动维护 Magic Number 列表
- 新文件类型需要手动添加
- 某些复杂格式（如 Office Open XML）难以准确识别

**实现要求**:
- 补充缺失的 Magic Number（如需要）
- 添加更多文件类型支持
- 优化检测逻辑
- 添加单元测试确保准确性

#### 2.3 混合方案（推荐）

**策略**: Magic Number + Tika 混合检测
- 第一步：使用 Magic Number 快速检测（90% 场景）
- 第二步：Magic Number 无法识别或不确定时，使用 Tika 检测
- 缓存机制：缓存检测结果，避免重复检测

---

## 🔧 技术规格

### 3. 方案 A：启用 Apache Tika

#### 3.1 依赖配置

**pom.xml 更新**:
```xml
<!-- Apache Tika (文件类型检测) -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
    <version>2.9.1</version>
</dependency>
<!-- 可选：如果需要元数据提取 -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-parsers-standard-package</artifactId>
    <version>2.9.1</version>
</dependency>
```

#### 3.2 服务接口设计

```java
/**
 * MIME 类型检测服务
 */
public interface MimeDetectionService {
    /**
     * 检测文件 MIME 类型
     * 
     * @param file 文件
     * @param filename 文件名（用于辅助判断）
     * @return MIME 类型（如：application/pdf）
     */
    String detectMimeType(File file, String filename);
    
    /**
     * 检测文件 MIME 类型（InputStream）
     */
    String detectMimeType(InputStream inputStream, String filename);
    
    /**
     * 验证文件类型是否匹配扩展名
     * 
     * @param file 文件
     * @param expectedExtension 期望的扩展名（如：pdf）
     * @return 是否匹配
     */
    boolean validateFileType(File file, String expectedExtension);
}
```

#### 3.3 实现类设计

```java
@Service
public class TikaMimeDetectionService implements MimeDetectionService {
    
    private final Tika tika;
    private final FileMagicValidator magicValidator; // 保留原有实现
    
    public TikaMimeDetectionService() {
        this.tika = new Tika();
        this.magicValidator = new FileMagicValidator();
    }
    
    @Override
    public String detectMimeType(File file, String filename) {
        // 1. 优先使用 Magic Number 快速检测
        try {
            String magicMime = magicValidator.detectMimeType(file);
            if (magicMime != null) {
                return magicMime;
            }
        } catch (Exception e) {
            log.warn("Magic Number 检测失败，使用 Tika: {}", e.getMessage());
        }
        
        // 2. 使用 Tika 检测（兜底）
        try {
            return tika.detect(file, filename);
        } catch (Exception e) {
            log.error("Tika 检测失败: {}", e.getMessage());
            throw new BusinessException("无法检测文件类型: " + e.getMessage());
        }
    }
    
    @Override
    public boolean validateFileType(File file, String expectedExtension) {
        String detectedMime = detectMimeType(file, null);
        String expectedMime = getMimeTypeByExtension(expectedExtension);
        return Objects.equals(detectedMime, expectedMime);
    }
    
    private String getMimeTypeByExtension(String extension) {
        // 扩展名到 MIME 类型的映射
        Map<String, String> mimeMap = Map.of(
            "pdf", "application/pdf",
            "ofd", "application/ofd",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            // ... 更多映射
        );
        return mimeMap.get(extension.toLowerCase());
    }
}
```

#### 3.4 集成到四性检测服务

```java
@Service
public class FourNatureCoreServiceImpl {
    
    private final MimeDetectionService mimeDetectionService;
    
    /**
     * 可用性检测（文件类型验证）
     */
    public AvailabilityCheckResult checkAvailability(File file, String filename) {
        // 验证文件类型
        String extension = getExtension(filename);
        boolean isValidType = mimeDetectionService.validateFileType(file, extension);
        
        if (!isValidType) {
            return AvailabilityCheckResult.failure(
                "文件类型不匹配：期望 " + extension + "，实际类型可能不符"
            );
        }
        
        // ... 其他可用性检测逻辑
        
        return AvailabilityCheckResult.success();
    }
}
```

### 4. 方案 B：强化 FileMagicValidator

#### 4.1 补充 Magic Number

**需要补充的文件类型**（如需要）:
- RTF（Rich Text Format）
- TXT（纯文本，UTF-8/GBK）
- CSV（CSV 文件）
- JSON（JSON 文件）
- 更多图片格式（WebP、BMP）

#### 4.2 优化检测逻辑

```java
@Component
public class EnhancedFileMagicValidator extends FileMagicValidator {
    
    /**
     * 增强的 MIME 类型检测
     */
    public String detectMimeType(File file) {
        // 1. 读取文件头部（前 32 字节通常足够）
        byte[] header = readFileHeader(file, 32);
        
        // 2. 遍历 Magic Number 列表进行匹配
        for (Map.Entry<String, byte[][]> entry : MAGIC_NUMBERS.entrySet()) {
            String extension = entry.getKey();
            byte[][] patterns = entry.getValue();
            
            for (byte[] pattern : patterns) {
                if (matchesPattern(header, pattern)) {
                    return extension;
                }
            }
        }
        
        // 3. 特殊处理：Office Open XML（需要检查 ZIP 内部结构）
        if (isZipFormat(header)) {
            String officeType = detectOfficeType(file);
            if (officeType != null) {
                return officeType;
            }
        }
        
        return null; // 无法识别
    }
    
    private boolean isZipFormat(byte[] header) {
        return header.length >= 4 && 
               header[0] == 0x50 && header[1] == 0x4B && 
               (header[2] == 0x03 || header[2] == 0x05 || header[2] == 0x07);
    }
    
    private String detectOfficeType(File file) {
        // 检查 ZIP 内部文件结构
        // DOCX: [Content_Types].xml 包含 word/
        // XLSX: [Content_Types].xml 包含 xl/
        // PPTX: [Content_Types].xml 包含 ppt/
        // ... 实现逻辑
    }
}
```

---

## 🧪 测试要求

### 5.1 单元测试

**测试用例**:
- 各种文件类型的正确识别
- 文件扩展名与内容不符的检测
- 损坏文件的处理
- 空文件的处理
- 恶意文件（伪装文件）的拦截

**测试数据准备**:
- 准备标准测试文件库（PDF、OFD、Office、图片等）
- 准备恶意测试文件（.exe 伪装成 .pdf）
- 准备边界情况文件（空文件、损坏文件）

### 5.2 集成测试

**测试场景**:
- 文件上传流程中的 MIME 检测
- 四性检测服务中的文件类型验证
- 性能测试（检测速度）

### 5.3 性能基准

**性能要求**:
- Magic Number 检测: < 1ms
- Tika 检测: < 50ms（首次）< 10ms（缓存后）
- 文件上传性能: 不受影响（< 5% 性能损失）

---

## 📝 开发检查清单

### 方案 A（启用 Tika）

- [ ] 评估当前 `FileMagicValidator` 的覆盖度和准确性
- [ ] 准备测试用例和测试文件
- [ ] 决策：选择方案 A 或方案 B
- [ ] 取消 `pom.xml` 中 Tika 依赖注释
- [ ] 创建 `MimeDetectionService` 接口
- [ ] 实现 `TikaMimeDetectionService`
- [ ] 集成混合检测策略（Magic Number + Tika）
- [ ] 更新 `FourNatureCoreServiceImpl` 使用新的检测服务
- [ ] 编写单元测试
- [ ] 编写集成测试
- [ ] 性能测试和优化
- [ ] 更新相关文档

### 方案 B（强化 FileMagicValidator）

- [ ] 评估当前覆盖度，确定需要补充的文件类型
- [ ] 补充缺失的 Magic Number
- [ ] 优化检测逻辑（Office Open XML 特殊处理）
- [ ] 增强错误处理
- [ ] 编写单元测试
- [ ] 性能测试
- [ ] 更新相关文档

---

## 🔗 相关文档

- 开发路线图：`docs/planning/development_roadmap_v1.0.md`
- 缺口分析报告：`docs/reports/roadmap-gap-analysis-2025-01.md`
- 四性检测服务：`nexusarchive-java/src/main/java/com/nexusarchive/service/impl/FourNatureCoreServiceImpl.java`
- FileMagicValidator：`nexusarchive-java/src/main/java/com/nexusarchive/util/FileMagicValidator.java`
- Apache Tika 文档：https://tika.apache.org/

---

**文档状态**: ✅ 已完成  
**下一步**: 进行评估，选择方案后开始开发实现

