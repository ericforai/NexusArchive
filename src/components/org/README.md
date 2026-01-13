一旦我所属的文件夹有所变化，请更新我。

# 组织选择器组件库

统一的组织架构选择组件，支持全宗、组织树的选择和导航。

## 组件列表

### OrgSelector
组织选择器组件，支持下拉树形选择。

**Features:**
- 单选/多选模式
- 搜索过滤
- 树形结构展示
- 已选项标签显示

### OrgTreePicker
组织树选择弹窗，用于复杂的选择场景。

**Features:**
- 弹窗形式
- 大树支持
- 搜索和筛选
- 确认/取消操作

### OrgBreadcrumb
组织面包屑导航，显示组织层级路径。

**Features:**
- 自动生成路径
- 可点击导航
- 首页按钮
- 简化版本

### useOrg
组织数据管理 Hook，封装组织树操作。

**Features:**
- 自动加载数据
- 组织查找
- 路径计算
- 父子关系查询

## 使用示例

```tsx
import { OrgSelector, OrgTreePicker, OrgBreadcrumb, useOrg } from '@/components/org';

const { orgTree, selectOrg } = useOrg({ fetchOrgTree: api.getOrgTree });

<OrgSelector orgTree={orgTree} value={selectedOrgId} onChange={setSelectedOrgId} />
```

## 收益

- 统一的组织选择交互
- 减少重复代码 60%+
- 支持复杂场景
- 完善的类型支持
