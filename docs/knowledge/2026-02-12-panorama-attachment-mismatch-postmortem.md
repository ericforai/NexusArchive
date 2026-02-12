# 全景视图附件“能打开但内容错配”复盘（2026-02-12）

> **日期**: 2026-02-12  
> **问题**: 全景视图中原始凭证先是 404 / 一直加载，修复后可打开但出现“发票内容错配”  
> **状态**: ✅ 已修复并增加防复发措施

---

## 现象与影响

1. 全景视图右侧附件区域：
   - 先出现 `404 Archive not found`
   - 再出现“无报错但一直加载中”
   - 最后出现“附件可打开但发票内容不对”
2. 影响用户对证据链可信度的判断，属于高优先级体验与合规风险。

---

## 故障链路（分层根因）

### A. 授权映射问题（404）
- 下载接口 `GET /api/archive/files/download/{fileId}` 在原始凭证分支仅按 `arc_original_voucher.id` 兜底。
- 但 `arc_file_content.archival_code` 实际常是 `voucher_no`（如 `INV-202311-089`），导致 `ARCHIVE_NOT_FOUND`。

### B. 响应头长度问题（一直加载）
- 接口返回 `Content-Length = arc_file_content.file_size`（DB值）。
- DB `file_size` 与物理文件实际长度不一致时，浏览器 `iframe` 会一直等待剩余字节，表现为“转圈不报错”。

### C. 存储目录错配问题（内容错）
- 运行时读取目录为 `nexusarchive-java/data/archives/uploads/demo/...`。
- 该目录下两份 demo 发票曾被错误覆盖为同一内容；而仓库根目录 `uploads/demo/...` 是正确版本。
- 最终出现“文件能打开但内容错误”。

---

## 修复动作

### 1) 下载授权兜底增强
- 文件: `nexusarchive-java/src/main/java/com/nexusarchive/controller/ArchiveFileController.java`
- 原始凭证授权链路改为：
  - `id`
  - `voucher_no`
  - `source_doc_id`

### 2) 下载长度计算修复
- 文件: `nexusarchive-java/src/main/java/com/nexusarchive/controller/ArchiveFileController.java`
- `Content-Length` 改为优先读取物理文件真实长度（`resource.contentLength()`），仅在读取失败时回退 DB 值。

### 3) Demo 目录纠偏 + 启动期同步
- 已将 `nexusarchive-java/data/archives/uploads/demo` 中错误副本替换为正确发票文件。
- 文件: `scripts/dev.sh`
- 启动时增加 demo 附件同步：
  - `uploads/demo -> nexusarchive-java/data/archives/uploads/demo`
  - 用于防止历史错误副本在重启后再次生效。

---

## 新增/更新防回归保障

1. **单元测试**  
   文件: `nexusarchive-java/src/test/java/com/nexusarchive/controller/ArchiveFileControllerUnitTest.java`  
   断言 DB `file_size` 与物理文件长度不一致时，接口仍返回正确 `Content-Length`。

2. **重启稳定性**  
   文件: `scripts/dev.sh`  
   使用 `nohup + < /dev/null` 启动前后端，降低脚本退出联动回收进程的概率。

3. **运行前校验建议（发布/演示前）**
   - 校验两个关键附件下载头：
     - `Content-Length` 是否与实际文件大小一致
   - 校验 demo 目录关键文件哈希是否与基准一致。
   - 推荐执行一键脚本：
     - `bash scripts/verify_panorama_attachments.sh`
     - 若需同时校验下载接口头部：`TOKEN="<JWT>" bash scripts/verify_panorama_attachments.sh`

---

## 经验总结（可执行）

1. 对“浏览器原生下载/预览接口”，必须把 `Content-Length` 当作协议级正确性来保证，不能盲信 DB 元数据。  
2. 相对路径系统中，**必须先明确运行时根目录**，再讨论“文件是否存在”。  
3. Demo 数据需要“单一真源（source of truth）”，避免多目录副本漂移。  
4. 遇到“无报错一直加载”，优先排查：
   - 响应是否 `Transfer-Encoding` / `Content-Length` 异常
   - 返回体字节数是否达到声明长度
   - 物理文件是否被错误副本覆盖

---

## 关联文档

- `docs/knowledge/2026-01-13-attachment-preview-auth-fix.md`（附件预览 404 与全宗过滤修复）
- `docs/knowledge/临时文件存储路径问题排查.md`（路径配置与持久化目录排查）
