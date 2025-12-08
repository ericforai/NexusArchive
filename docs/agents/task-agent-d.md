# Agent D: 基础设施工程师任务书

> **角色**: 基础设施工程师
> **技术栈**: Java, Elasticsearch, OFD处理, ERP集成
> **负责阶段**: 第二阶段（OFD转换）+ 第三阶段（搜索、ERP）
> **前置依赖**: ⚠️ 需等待 Agent A 完成安全配置，Agent B 完成签章集成

---

## 📋 项目背景

NexusArchive 需要集成多个外部系统和处理能力：
- **OFD 转换**：将 PDF 转换为 OFD 格式长期保存
- **全文检索**：集成 Elasticsearch 实现档案内容搜索
- **ERP 集成**：对接多种 ERP 系统（目前仅支持 YonSuite）

### 关键约束
- **信创适配**：OFD 是国产版式文档格式，必须支持
- **私有部署**：所有服务可离线运行
- **可扩展性**：ERP 适配器需支持多种 ERP

---

## 🔐 必读规则

执行任务前，请阅读以下规则文件：

1. **[.agent/rules/general.md](file:///Users/user/nexusarchive/.agent/rules/general.md)** - 核心编码规范

---

## ✅ 任务清单

### 2.6 OFD 长期保存格式

| 序号 | 任务 | 产出文件 | 说明 | 验收标准 |
|------|------|----------|------|----------|
| 2.6.1 | PDF转OFD服务 | `OfdConvertService.java` | 格式转换 | 转换后文件可打开 |
| 2.6.2 | OFD阅读器集成 | 前端集成 ofd.js | 前端预览OFD | 浏览器可预览 |
| 2.6.3 | 批量转换任务 | `OfdConvertJob.java` | 异步批量处理 | 后台任务正常运行 |
| 2.6.4 | 转换日志记录 | `arc_convert_log` 表 | 记录转换结果 | 日志完整 |

**依赖说明：**
```xml
<!-- pom.xml 添加 -->
<dependency>
    <groupId>org.ofdrw</groupId>
    <artifactId>ofdrw-converter</artifactId>
    <version>2.2.6</version>
</dependency>
<dependency>
    <groupId>org.ofdrw</groupId>
    <artifactId>ofdrw-reader</artifactId>
    <version>2.2.6</version>
</dependency>
```

**代码示例：**
```java
// OfdConvertService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class OfdConvertService {
    
    private final FileStorageService storageService;
    private final ConvertLogMapper convertLogMapper;
    
    /**
     * 将 PDF 转换为 OFD
     */
    public ConvertResult pdfToOfd(String pdfPath, String archiveId) {
        try {
            Path sourcePath = storageService.resolvePath(pdfPath);
            Path targetPath = sourcePath.resolveSibling(
                sourcePath.getFileName().toString().replace(".pdf", ".ofd"));
            
            // 使用 OFDRW 进行转换
            try (ConvertHelper converter = new ConvertHelper(sourcePath, targetPath)) {
                converter.convert();
            }
            
            // 记录日志
            ConvertLog log = ConvertLog.builder()
                .archiveId(archiveId)
                .sourceFormat("PDF")
                .targetFormat("OFD")
                .sourcePath(pdfPath)
                .targetPath(targetPath.toString())
                .status("SUCCESS")
                .convertTime(LocalDateTime.now())
                .build();
            convertLogMapper.insert(log);
            
            return ConvertResult.success(targetPath.toString());
            
        } catch (Exception e) {
            log.error("PDF转OFD失败: {}", e.getMessage(), e);
            return ConvertResult.fail(e.getMessage());
        }
    }
}
```

**前端 OFD 预览：**
```typescript
// 安装 ofd.js
// npm install ofd.js

// OfdViewer.tsx
import { useEffect, useRef } from 'react'
import { parseOfdFile, renderOfd } from 'ofd.js'

interface OfdViewerProps {
  fileUrl: string
}

export function OfdViewer({ fileUrl }: OfdViewerProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  
  useEffect(() => {
    let mounted = true
    
    async function loadOfd() {
      const response = await fetch(fileUrl)
      const arrayBuffer = await response.arrayBuffer()
      const ofdDoc = await parseOfdFile(arrayBuffer)
      
      if (mounted && containerRef.current) {
        await renderOfd(containerRef.current, ofdDoc)
      }
    }
    
    loadOfd()
    
    return () => { mounted = false }
  }, [fileUrl])
  
  return <div ref={containerRef} className="ofd-container" />
}
```

---

### 3.2 全文检索优化

| 序号 | 任务 | 产出文件 | 说明 | 验收标准 |
|------|------|----------|------|----------|
| 3.2.1 | Elasticsearch 集成 | 配置和连接 | ES 客户端配置 | 连接成功 |
| 3.2.2 | 档案索引服务 | `ArchiveIndexService.java` | 建立索引 | 索引创建成功 |
| 3.2.3 | 全文搜索接口 | `SearchController.java` | 搜索API | 搜索返回结果 |
| 3.2.4 | OCR文本索引 | 异步任务 | 附件内容索引 | OCR 文本可搜 |
| 3.2.5 | 搜索结果高亮 | 前端展示 | 关键词高亮 | UI 显示高亮 |

**ES 配置：**
```yaml
# application.yml
elasticsearch:
  hosts: ${ES_HOSTS:localhost:9200}
  username: ${ES_USERNAME:}
  password: ${ES_PASSWORD:}
  connect-timeout: 5000
  socket-timeout: 30000
```

**代码示例：**
```java
// ElasticsearchConfig.java
@Configuration
@RequiredArgsConstructor
public class ElasticsearchConfig {
    
    @Value("${elasticsearch.hosts}")
    private String hosts;
    
    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = RestClient.builder(
            HttpHost.create(hosts))
            .build();
        
        ElasticsearchTransport transport = new RestClientTransport(
            restClient, new JacksonJsonpMapper());
        
        return new ElasticsearchClient(transport);
    }
}

// ArchiveIndexService.java
@Service
@RequiredArgsConstructor
@Slf4j  
public class ArchiveIndexService {
    
    private static final String INDEX_NAME = "nexus_archives";
    
    private final ElasticsearchClient esClient;
    
    public void indexArchive(Archive archive) {
        try {
            ArchiveDocument doc = ArchiveDocument.builder()
                .id(archive.getId())
                .title(archive.getTitle())
                .archivalCode(archive.getArchivalCode())
                .summary(archive.getSummary())
                .categoryCode(archive.getCategoryCode())
                .createdTime(archive.getCreatedTime())
                // OCR 文本内容
                .fullText(archive.getOcrText())
                .build();
            
            esClient.index(i -> i
                .index(INDEX_NAME)
                .id(archive.getId())
                .document(doc)
            );
        } catch (Exception e) {
            log.error("索引档案失败: {}", archive.getId(), e);
        }
    }
    
    public SearchResult<ArchiveDocument> search(String keyword, int page, int size) {
        try {
            SearchResponse<ArchiveDocument> response = esClient.search(s -> s
                .index(INDEX_NAME)
                .query(q -> q
                    .multiMatch(m -> m
                        .query(keyword)
                        .fields("title^3", "summary^2", "fullText", "archivalCode")
                    )
                )
                .highlight(h -> h
                    .fields("title", f -> f)
                    .fields("summary", f -> f)
                    .fields("fullText", f -> f)
                    .preTags("<em class='highlight'>")
                    .postTags("</em>")
                )
                .from(page * size)
                .size(size),
                ArchiveDocument.class
            );
            
            return new SearchResult<>(
                response.hits().hits().stream()
                    .map(hit -> {
                        ArchiveDocument doc = hit.source();
                        // 应用高亮
                        if (hit.highlight() != null) {
                            hit.highlight().forEach((field, highlights) -> {
                                // 设置高亮内容
                            });
                        }
                        return doc;
                    })
                    .collect(Collectors.toList()),
                response.hits().total().value()
            );
        } catch (Exception e) {
            log.error("搜索失败: {}", keyword, e);
            return SearchResult.empty();
        }
    }
}
```

---

### 3.3 更多 ERP 集成

| 序号 | 任务 | 产出文件 | 说明 | 验收标准 |
|------|------|----------|------|----------|
| 3.3.1 | ERP适配器接口 | `ErpAdapter.java` | 统一接口定义 | 接口设计合理 |
| 3.3.2 | 金蝶云星空适配 | `KingdeeAdapter.java` | 金蝶集成 | 可获取凭证 |
| 3.3.3 | 通用API适配 | `GenericErpAdapter.java` | 标准REST接口 | 支持自定义配置 |
| 3.3.4 | ERP配置管理 | 管理界面 | 多ERP配置管理 | UI可用 |

**代码示例：**
```java
// ErpAdapter.java
public interface ErpAdapter {
    
    /**
     * 获取适配器标识
     */
    String getIdentifier();
    
    /**
     * 获取适配器名称
     */
    String getName();
    
    /**
     * 测试连接
     */
    ConnectionTestResult testConnection(ErpConfig config);
    
    /**
     * 同步凭证
     */
    List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取单个凭证详情
     */
    VoucherDTO getVoucherDetail(ErpConfig config, String voucherNo);
    
    /**
     * 获取附件
     */
    List<AttachmentDTO> getAttachments(ErpConfig config, String voucherNo);
}

// KingdeeAdapter.java
@Service("kingdee")
@Slf4j
public class KingdeeAdapter implements ErpAdapter {
    
    @Override
    public String getIdentifier() { return "kingdee"; }
    
    @Override
    public String getName() { return "金蝶云星空"; }
    
    @Override
    public List<VoucherDTO> syncVouchers(ErpConfig config, 
            LocalDate startDate, LocalDate endDate) {
        // 金蝶 API 调用实现
        // POST /k3cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser
        // POST /k3cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery
        return null;
    }
}

// ErpAdapterFactory.java
@Component
@RequiredArgsConstructor
public class ErpAdapterFactory {
    
    private final Map<String, ErpAdapter> adapters;
    
    public ErpAdapter getAdapter(String type) {
        ErpAdapter adapter = adapters.get(type);
        if (adapter == null) {
            throw new IllegalArgumentException("不支持的 ERP 类型: " + type);
        }
        return adapter;
    }
    
    public List<ErpAdapterInfo> listAvailableAdapters() {
        return adapters.values().stream()
            .map(a -> new ErpAdapterInfo(a.getIdentifier(), a.getName()))
            .collect(Collectors.toList());
    }
}
```

---

## 🧪 验证步骤

### 1. OFD 转换验证
```bash
# 上传 PDF 后调用转换
curl -X POST http://localhost:8080/api/archive/{id}/convert-to-ofd \
  -H "Authorization: Bearer $TOKEN"
```

### 2. ES 搜索验证
```bash
# 搜索档案
curl "http://localhost:8080/api/search?q=发票&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

### 3. ERP 适配器验证
```bash
# 列出可用适配器
curl http://localhost:8080/api/admin/erp/adapters
```

---

## 📝 完成标志

任务完成后，请在 `docs/优化计划.md` 中勾选：
- 第二阶段 2.6 OFD相关项目
- 第三阶段 3.2、3.3 相关项目

---

*Agent D 任务书 - 由 Claude 于 2025-12-07 生成*
