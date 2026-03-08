# 系统全面自查矩阵

- 生成时间: 2026-03-05T06:25:54.171Z
- 模块总数: 41
- 有测试映射: 28
- 无测试映射: 13
- 状态分布: passed=0, failed=11, mapped_not_run=16, uncovered=13, skipped_only=1

## 模块状态

| 模块ID | 状态 | 映射测试文件数 | 有运行状态文件数 | 失败断言数 | Manifest |
| --- | --- | ---: | ---: | ---: | --- |
| component.admin | mapped_not_run | 1 | 0 | 0 | `src/components/admin/manifest.config.ts` |
| component.archive | failed | 12 | 3 | 8 | `src/components/archive/manifest.config.ts` |
| component.auth | failed | 4 | 2 | 1 | `src/components/auth/manifest.config.ts` |
| component.common | uncovered | 0 | 0 | 0 | `src/components/common/manifest.config.ts` |
| component.debug | mapped_not_run | 2 | 0 | 0 | `src/components/debug/manifest.config.ts` |
| component.layout | mapped_not_run | 1 | 0 | 0 | `src/components/layout/manifest.config.ts` |
| component.matching | uncovered | 0 | 0 | 0 | `src/components/matching/manifest.config.ts` |
| component.modals | mapped_not_run | 1 | 0 | 0 | `src/components/modals/manifest.config.ts` |
| component.operations | mapped_not_run | 4 | 0 | 0 | `src/components/operations/manifest.config.ts` |
| component.org | failed | 1 | 1 | 2 | `src/components/org/manifest.config.ts` |
| component.pages | uncovered | 0 | 0 | 0 | `src/components/pages/manifest.config.ts` |
| component.panorama | uncovered | 0 | 0 | 0 | `src/components/panorama/manifest.config.ts` |
| component.preview | failed | 8 | 2 | 2 | `src/components/preview/manifest.config.ts` |
| component.scan | uncovered | 0 | 0 | 0 | `src/components/scan/manifest.config.ts` |
| component.settings | mapped_not_run | 14 | 0 | 0 | `src/components/settings/manifest.config.ts` |
| component.table | mapped_not_run | 4 | 0 | 0 | `src/components/table/manifest.config.ts` |
| component.voucher | mapped_not_run | 6 | 0 | 0 | `src/components/voucher/manifest.config.ts` |
| component.watermark | failed | 1 | 1 | 1 | `src/components/watermark/manifest.config.ts` |
| feature.archives | failed | 8 | 2 | 8 | `src/features/archives/manifest.config.ts` |
| feature.borrowing | failed | 1 | 1 | 1 | `src/features/borrowing/manifest.config.ts` |
| feature.compliance | uncovered | 0 | 0 | 0 | `src/features/compliance/manifest.config.ts` |
| feature.integration-settings | mapped_not_run | 14 | 0 | 0 | `src/components/settings/integration/manifest.config.ts` |
| feature.settings | mapped_not_run | 14 | 0 | 0 | `src/features/settings/manifest.config.ts` |
| nexusarchive.collection-batch | skipped_only | 12 | 1 | 0 | `nexusarchive-java/src/main/java/com/nexusarchive/collection/manifest.config.ts` |
| page.admin | mapped_not_run | 1 | 0 | 0 | `src/pages/admin/manifest.config.ts` |
| page.archives | failed | 8 | 2 | 8 | `src/pages/archives/manifest.config.ts` |
| page.audit | failed | 3 | 3 | 6 | `src/pages/audit/manifest.config.ts` |
| page.auth | failed | 4 | 2 | 1 | `src/pages/Auth/manifest.config.ts` |
| page.collection | mapped_not_run | 1 | 0 | 0 | `src/pages/collection/manifest.config.ts` |
| page.debug | mapped_not_run | 2 | 0 | 0 | `src/pages/debug/manifest.config.ts` |
| page.demo | uncovered | 0 | 0 | 0 | `src/pages/demo/manifest.config.ts` |
| page.matching | uncovered | 0 | 0 | 0 | `src/pages/matching/manifest.config.ts` |
| page.operations | mapped_not_run | 4 | 0 | 0 | `src/pages/operations/manifest.config.ts` |
| page.panorama | uncovered | 0 | 0 | 0 | `src/pages/panorama/manifest.config.ts` |
| page.portal | uncovered | 0 | 0 | 0 | `src/pages/portal/manifest.config.ts` |
| page.pre-archive | failed | 20 | 5 | 10 | `src/pages/pre-archive/manifest.config.ts` |
| page.security | uncovered | 0 | 0 | 0 | `src/pages/security/manifest.config.ts` |
| page.settings | mapped_not_run | 14 | 0 | 0 | `src/pages/settings/manifest.config.ts` |
| page.stats | uncovered | 0 | 0 | 0 | `src/pages/stats/manifest.config.ts` |
| page.utilization | mapped_not_run | 1 | 0 | 0 | `src/pages/utilization/manifest.config.ts` |
| shared.utils | uncovered | 0 | 0 | 0 | `src/utils/manifest.config.ts` |

## 无测试映射模块

- component.common (src/components/common/manifest.config.ts)
- component.matching (src/components/matching/manifest.config.ts)
- component.pages (src/components/pages/manifest.config.ts)
- component.panorama (src/components/panorama/manifest.config.ts)
- component.scan (src/components/scan/manifest.config.ts)
- feature.compliance (src/features/compliance/manifest.config.ts)
- page.demo (src/pages/demo/manifest.config.ts)
- page.matching (src/pages/matching/manifest.config.ts)
- page.panorama (src/pages/panorama/manifest.config.ts)
- page.portal (src/pages/portal/manifest.config.ts)
- page.security (src/pages/security/manifest.config.ts)
- page.stats (src/pages/stats/manifest.config.ts)
- shared.utils (src/utils/manifest.config.ts)

## 使用说明

- 执行 `npm run self-check:run` 触发持续回归并产出本矩阵。
- 执行 `npm run self-check:matrix` 仅根据现有测试清单重建矩阵（不跑测试）。
