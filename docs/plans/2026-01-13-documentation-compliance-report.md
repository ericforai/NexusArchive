# 目录文档合规性检查报告

**日期**: 2026-01-13
**检查者**: Virtual Expert (Antigravity)
**状态**: 🔴 不合规 (FAILED)

## 1. 检查概要

基于项目规则 `general.md` 第 7 节 "文档自洽规则" 进行全量目录扫描，发现大量由于缺失 `README.md` 或头部声明不规范导致的违规项。

### 统计数据（估算）
- **缺失 README.md 的目录**: > 200 个
- **头部声明错误的 README.md**: > 50 个
- **受影响主要区域**: `src`, `nexus-core`, `nexusarchive-java`, `docs`

## 2. 主要违规项分析

### 2.1 严重缺失 (Missing README.md)
以下核心代码目录深层结构完全缺乏文档描述：

*   **Java Backend**:
    *   `nexus-core/src/main/java/...`
    *   `nexusarchive-java/src/main/java/...`
    *   几乎所有包（package）对应目录均无说明。
*   **Frontend**:
    *   `src/components/...`
    *   `src/features/...`
    *   `src/pages/...`
*   **其他**:
    *   `reports/tests`
    *   `backup_...` (建议加入白名单或清理)

### 2.2 格式错误 (Invalid Header)
大量现存 `README.md` 未包含强制性头部声明：
> `一旦我所属的文件夹有所变化，请更新我。`

**典型案例**:
*   `docs/metrics`
*   `docs/architecture`
*   `docs/release`
*   `scripts/git-hooks`
*   `src/api`
*   `src/auth`

## 3. 整改建议 (Action Plan)

1.  **批量修复头部声明**:
    *   遍历所有现有 `README.md`，追加或修正第一行声明。
2.  **代码目录补全**:
    *   对 Java 包目录和 Frontend 组件目录，生成模板化 `README.md`。
    *   内容应包含：当前包/组件的职责摘要。
3.  **修订白名单**:
    *   在相关脚本或规范中明确 `backup_*`, `reports` 等非源码目录是否豁免。

## 4. 详细违规清单 (部分快照)

### 头部声明错误 (Header Missing)
- `docs/testing/README.md`
- `src/store/README.md`
- `nexus-core/src/main/java/com/nexusarchive/core/compliance/README.md`
- ...

### 文件缺失 (File Missing)
- `src/types/README.md`
- `src/config/README.md`
- `nexus-core/src/main/java/com/nexusarchive/core/domain/README.md`
- ...
