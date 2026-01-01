// Input: React, lucide-react, Ant Design
// Output: DataTable 组件
// Pos: 通用复用组件 - 数据表格

import React, { useMemo } from 'react';
import { Table, TableProps as AntTableProps } from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react';

export interface ColumnType<T = any> {
  key: string;
  title: string;
  dataIndex?: string | string[];
  render?: (value: any, record: T, index: number) => React.ReactNode;
  width?: number;
  align?: 'left' | 'center' | 'right';
  fixed?: 'left' | 'right';
  sorter?: boolean | ((a: T, b: T) => number);
  filters?: Array<{ text: string; value: any }>;
  onFilter?: (value: any, record: T) => boolean;
}

export interface DataTableProps<T = any> {
  columns: ColumnType<T>[];
  dataSource: T[];
  rowKey?: string | ((record: T) => string);
  loading?: boolean;
  pagination?: false | TablePaginationConfig;
  onRow?: (record: T, index?: number) => {
    onClick?: (e: React.MouseEvent) => void;
    onDoubleClick?: (e: React.MouseEvent) => void;
    [key: string]: any;
  };
  scroll?: { x?: number; y?: number };
  className?: string;
  size?: 'large' | 'middle' | 'small';
  bordered?: boolean;
}

/**
 * 统一的数据表格组件
 * <p>
 * 基于 Ant Design Table，提供统一的样式和分页配置
 * </p>
 */
export function DataTable<T extends Record<string, any>>({
  columns,
  dataSource,
  rowKey = 'id',
  loading = false,
  pagination = { pageSize: 10, showSizeChanger: true, showTotal: (total: number) => `共 ${total} 条` },
  onRow,
  scroll,
  className = '',
  size = 'middle',
  bordered = false,
}: DataTableProps<T>) {
  // Convert columns to Ant Design format
  const antColumns: ColumnsType<T> = useMemo(() => {
    return columns.map((col) => ({
      key: col.key,
      title: col.title,
      dataIndex: col.dataIndex,
      render: col.render,
      width: col.width,
      align: col.align || 'left',
      fixed: col.fixed,
      sorter: col.sorter,
      filters: col.filters,
      onFilter: col.onFilter,
    }));
  }, [columns]);

  // Custom pagination icons
  const itemRender: AntTableProps['pagination']['itemRender'] = (
    current,
    type,
    originalElement
  ) => {
    if (type === 'prev') {
      return <ChevronLeft className="w-4 h-4" />;
    }
    if (type === 'next') {
      return <ChevronRight className="w-4 h-4" />;
    }
    if (type === 'page') {
      return <span className="px-2">{current}</span>;
    }
    return originalElement;
  };

  const mergedPagination = pagination === false ? false : {
    ...pagination,
    itemRender,
  };

  return (
    <div className={`data-table ${className}`}>
      <Table<T>
        columns={antColumns}
        dataSource={dataSource}
        rowKey={rowKey}
        loading={loading}
        pagination={mergedPagination}
        onRow={onRow}
        scroll={scroll}
        size={size}
        bordered={bordered}
        className="custom-table"
      />
    </div>
  );
}

export default DataTable;
