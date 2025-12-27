# engine/matching/

智能凭证关联规则引擎模块。

一旦我所属的文件夹有所变化，请更新我。

## 功能描述

基于业务属性自动匹配记账凭证与原始凭证，输出 Must-Link/Should-Link/May-Link 关联关系。

## 核心组件

| 文件 | 地位 | 功能 |
|------|------|------|
| `VoucherMatchingEngine.java` | 主引擎 | 业务场景识别、候选召回、评分、结果生成 |
| `CandidateFinder.java` | 核心组件 | 候选查找（索引粗筛+内存精筛） |
| `MatchingScorer.java` | 核心组件 | 多维度评分（金额/日期/交易对手） |
| `FuzzyMatcher.java` | 核心组件 | 模糊匹配（精确/包含/相似度/数值容差） |
| `RuleTemplateManager.java` | 核心组件 | 规则模板加载和缓存 |
| `OnboardingService.java` | 服务 | 初始化向导（扫描+自动映射） |

## 枚举类

| 枚举 | 说明 |
|------|------|
| `EvidenceRole` | 证据角色（授权/结算/税务/合同/执行/记账触发） |
| `AccountRole` | 科目角色（现金/银行/应收/应付/费用/收入等） |
| `BusinessScene` | 业务场景（付款/收款/报销/采购/销售等15种） |
| `MatchStrategy` | 匹配策略（精确/包含/相似度/数值容差） |
| `MatchStatus` | 匹配状态（处理中/已匹配/待补证/需确认） |
| `LinkType` | 关联类型（必关联/应关联/可关联） |

## DTO 类

| DTO | 说明 |
|-----|------|
| `MatchResult` | 匹配结果 |
| `LinkResult` | 单个证据角色的关联结果 |
| `ScoredCandidate` | 评分后的候选文档 |
| `MatchingContext` | 匹配上下文 |
| `RuleTemplate` | 规则模板 |
| `VoucherData` | 凭证数据 |
| `BusinessAttributes` | 业务属性识别结果 |
| `OnboardingSummary` | 初始化扫描摘要 |
| `AutoMappingResult` | 自动映射结果 |
| `UnmatchedItem` | 未匹配项 |
| `MappingConfirmation` | 映射确认请求 |

## API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/matching/execute/{voucherId}` | 异步执行单凭证匹配 |
| POST | `/api/matching/execute/batch` | 批量匹配 |
| GET | `/api/matching/task/{taskId}` | 查询任务结果 |
| GET | `/api/matching/templates` | 获取模板列表 |
| POST | `/api/matching/templates/reload` | 刷新模板 |
| POST | `/api/matching/onboarding/scan/{companyId}` | 扫描客户数据 |
| POST | `/api/matching/onboarding/apply-preset/{companyId}` | 应用预置规则 |
| GET | `/api/matching/onboarding/pending/{companyId}` | 获取待确认项 |
| POST | `/api/matching/onboarding/confirm/{companyId}` | 确认映射 |
