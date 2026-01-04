一旦我所属的文件夹有所变化，请更新我。
本目录存放表格相关通用组件，提供统一的表格操作和数据管理能力。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `DataTable.tsx` | 通用组件 | 统一的数据表格组件，基于 Ant Design Table |
| `TableFilters.tsx` | 通用组件 | 表格筛选组件，支持多种字段类型 |
| `TableActions.tsx` | 通用组件 | 表格操作组件，提供下拉菜单形式的操作按钮 |
| `TablePreviewAction.tsx` | 通用组件 | 表格行预览操作组件，提供统一的预览按钮样式 |
| `useDataTable.ts` | Hook | 表格数据管理 Hook，封装分页、筛选、数据获取逻辑 |
| `index.ts` | 聚合入口 | 表格组件库统一导出 |

## 组件列表

### DataTable
统一的数据表格组件，基于 Ant Design Table。

**Features:**
- 统一分页配置
- 自定义列配置
- 排序和筛选支持
- 响应式滚动

**Usage:**
```tsx
import { DataTable } from '@/components/table';

const columns: ColumnType<Archive>[] = [
  { key: 'id', title: 'ID', dataIndex: 'id' },
  { key: 'title', title: '标题', dataIndex: 'title' },
];

<DataTable
  columns={columns}
  dataSource={data}
  loading={loading}
  pagination={{ pageSize: 10 }}
/>
```

### TableFilters
表格筛选组件，支持多种字段类型。

**Features:**
- 文本输入
- 下拉选择
- 日期选择
- 可折叠面板

**Usage:**
```tsx
import { TableFilters } from '@/components/table';

const fields: FilterField[] = [
  { key: 'title', label: '标题', type: 'text' },
  { key: 'status', label: '状态', type: 'select', options: [...] },
];

<TableFilters
  fields={fields}
  values={filters}
  onChange={setFilters}
  onSearch={handleSearch}
/>
```

### TableActions
表格操作组件，提供下拉菜单形式的操作按钮。

**Features:**
- 主要操作直接显示
- 更多操作收起到下拉菜单
- 支持禁用和危险操作

**Usage:**
```tsx
import { TableActions } from '@/components/table';

const actions: ActionItem[] = [
  { key: 'view', label: '查看', onClick: handleView },
  { key: 'edit', label: '编辑', onClick: handleEdit },
  { key: 'delete', label: '删除', onClick: handleDelete, danger: true },
];

<TableActions actions={actions} record={record} />
```

### TablePreviewAction
表格行预览操作组件，提供统一的预览按钮样式。

**Features:**
- 统一的预览按钮样式和交互
- 悬停高亮效果
- 可选的删除按钮
- 支持键盘导航（Tab、Enter）

**Usage:**
```tsx
import { TablePreviewAction } from '@/components/table';

<TablePreviewAction
  hovered={hoveredRowId === row.id}
  onPreview={() => handlePreview(row)}
  showDelete={false}
  onDelete={() => handleDelete(row.id)}
/>
```

**Props:**
- `hovered`: `boolean` - 是否悬停
- `onPreview`: `() => void` - 预览回调
- `showDelete`: `boolean` - 是否显示删除按钮
- `onDelete`: `() => void` - 删除回调（可选）
- `previewLabel`: `string` - 预览按钮文字（默认"查看"）

### useDataTable
表格数据管理 Hook，封装分页、筛选、数据获取逻辑。

**Features:**
- 自动分页管理
- 筛选条件管理
- 自动数据加载

**Usage:**
```tsx
import { useDataTable } from '@/components/table';

const { data, loading, pagination, handlePageChange, handleFilterChange } = useDataTable({
  fetchFn: async (page, pageSize, filters) => {
    const response = await api.fetchArchives({ page, pageSize, ...filters });
    return { data: response.data, total: response.total };
  },
  defaultPageSize: 10,
});
```

## 完整示例

```tsx
import { DataTable, TableFilters, TablePreviewAction, useDataTable } from '@/components/table';

function ArchiveList() {
  const {
    data,
    loading,
    pagination,
    filters,
    handlePageChange,
    handleFilterChange,
  } = useDataTable({
    fetchFn: fetchArchives,
  });

  const [hoveredRowId, setHoveredRowId] = useState<string | null>(null);

  const columns: ColumnType<Archive>[] = [
    { key: 'id', title: 'ID', dataIndex: 'id' },
    { key: 'title', title: '标题', dataIndex: 'title' },
    { key: 'amount', title: '金额', dataIndex: 'amount' },
    {
      key: 'actions',
      title: '操作',
      render: (_, record) => (
        <TablePreviewAction
          hovered={hoveredRowId === record.id}
          onPreview={() => handlePreview(record)}
          showDelete={canDelete}
          onDelete={() => handleDelete(record.id)}
        />
      ),
    },
  ];

  const filterFields: FilterField[] = [
    { key: 'title', label: '标题', type: 'text' },
    { key: 'status', label: '状态', type: 'select', options: [...] },
  ];

  return (
    <div>
      <TableFilters
        fields={filterFields}
        values={filters}
        onChange={handleFilterChange}
      />
      <DataTable
        columns={columns}
        dataSource={data}
        loading={loading}
        pagination={pagination}
        onChange={handlePageChange}
        onRow={(record) => ({
          onMouseEnter: () => setHoveredRowId(record.id),
          onMouseLeave: () => setHoveredRowId(null),
        })}
      />
    </div>
  );
}
```

## 收益

- 减少列表页面重复代码 70%+
- 统一的表格样式和交互
- 简化分页和筛选逻辑
- 提高开发效率
