# 系统全面自查矩阵

- 生成时间: 2026-03-05T06:39:30.222Z
- 模块总数: 41
- 有测试映射: 41
- 无测试映射: 0
- 状态分布: passed=36, failed=5, mapped_not_run=0, uncovered=0, skipped_only=0

## 模块状态

| 模块ID | 状态 | 映射测试文件数 | 有运行状态文件数 | 失败断言数 | Manifest |
| --- | --- | ---: | ---: | ---: | --- |
| component.admin | passed | 1 | 1 | 0 | `src/components/admin/manifest.config.ts` |
| component.archive | passed | 1 | 1 | 0 | `src/components/archive/manifest.config.ts` |
| component.auth | passed | 1 | 1 | 0 | `src/components/auth/manifest.config.ts` |
| component.common | passed | 1 | 1 | 0 | `src/components/common/manifest.config.ts` |
| component.debug | passed | 2 | 2 | 0 | `src/components/debug/manifest.config.ts` |
| component.layout | passed | 1 | 1 | 0 | `src/components/layout/manifest.config.ts` |
| component.matching | passed | 1 | 1 | 0 | `src/components/matching/manifest.config.ts` |
| component.modals | passed | 1 | 1 | 0 | `src/components/modals/manifest.config.ts` |
| component.operations | passed | 4 | 4 | 0 | `src/components/operations/manifest.config.ts` |
| component.org | passed | 1 | 1 | 0 | `src/components/org/manifest.config.ts` |
| component.pages | passed | 1 | 1 | 0 | `src/components/pages/manifest.config.ts` |
| component.panorama | passed | 1 | 1 | 0 | `src/components/panorama/manifest.config.ts` |
| component.preview | passed | 1 | 1 | 0 | `src/components/preview/manifest.config.ts` |
| component.scan | passed | 1 | 1 | 0 | `src/components/scan/manifest.config.ts` |
| component.settings | failed | 14 | 14 | 3 | `src/components/settings/manifest.config.ts` |
| component.table | passed | 4 | 4 | 0 | `src/components/table/manifest.config.ts` |
| component.voucher | failed | 6 | 6 | 1 | `src/components/voucher/manifest.config.ts` |
| component.watermark | passed | 1 | 1 | 0 | `src/components/watermark/manifest.config.ts` |
| feature.archives | passed | 1 | 1 | 0 | `src/features/archives/manifest.config.ts` |
| feature.borrowing | passed | 1 | 1 | 0 | `src/features/borrowing/manifest.config.ts` |
| feature.compliance | passed | 1 | 1 | 0 | `src/features/compliance/manifest.config.ts` |
| feature.integration-settings | failed | 14 | 14 | 3 | `src/components/settings/integration/manifest.config.ts` |
| feature.settings | failed | 14 | 14 | 3 | `src/features/settings/manifest.config.ts` |
| nexusarchive.collection-batch | passed | 12 | 11 | 0 | `nexusarchive-java/src/main/java/com/nexusarchive/collection/manifest.config.ts` |
| page.admin | passed | 1 | 1 | 0 | `src/pages/admin/manifest.config.ts` |
| page.archives | passed | 1 | 1 | 0 | `src/pages/archives/manifest.config.ts` |
| page.audit | passed | 1 | 1 | 0 | `src/pages/audit/manifest.config.ts` |
| page.auth | passed | 1 | 1 | 0 | `src/pages/Auth/manifest.config.ts` |
| page.collection | passed | 1 | 1 | 0 | `src/pages/collection/manifest.config.ts` |
| page.debug | passed | 2 | 2 | 0 | `src/pages/debug/manifest.config.ts` |
| page.demo | passed | 1 | 1 | 0 | `src/pages/demo/manifest.config.ts` |
| page.matching | passed | 1 | 1 | 0 | `src/pages/matching/manifest.config.ts` |
| page.operations | passed | 4 | 4 | 0 | `src/pages/operations/manifest.config.ts` |
| page.panorama | passed | 1 | 1 | 0 | `src/pages/panorama/manifest.config.ts` |
| page.portal | passed | 1 | 1 | 0 | `src/pages/portal/manifest.config.ts` |
| page.pre-archive | passed | 1 | 1 | 0 | `src/pages/pre-archive/manifest.config.ts` |
| page.security | passed | 1 | 1 | 0 | `src/pages/security/manifest.config.ts` |
| page.settings | failed | 14 | 14 | 3 | `src/pages/settings/manifest.config.ts` |
| page.stats | passed | 1 | 1 | 0 | `src/pages/stats/manifest.config.ts` |
| page.utilization | passed | 1 | 1 | 0 | `src/pages/utilization/manifest.config.ts` |
| shared.utils | passed | 1 | 1 | 0 | `src/utils/manifest.config.ts` |

## 无测试映射模块

- 无

## 使用说明

- 执行 `npm run self-check:run` 触发持续回归并产出本矩阵。
- 执行 `npm run self-check:matrix` 仅根据现有测试清单重建矩阵（不跑测试）。
