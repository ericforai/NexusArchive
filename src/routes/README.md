一旦我所属的文件夹有所变化，请更新我。
本目录存放前端路由定义。
用于页面路径与路由装配（仅引用 pages + layouts/common/auth）。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `__tests__/` | 测试 | 路由常量与映射规则测试 |
| `index.tsx` | 路由入口 | 路由表与路由配置 |
| `paths.ts` | 路径常量 | 路由路径枚举 |

## 调试路由补充

- `/system/debug/ofd-spike`：OFD 技术路线 spike 页面，验证 `liteofd` 与 `ofdrw` 两条替代方案。

## 测试说明

- `__tests__/paths.test.ts`：关键菜单键到路径映射断言。
- `__tests__/menu-routing-integrity.test.ts`：递归校验二级/三级叶子菜单均命中有效路由（不会落入 `*` 兜底）。
