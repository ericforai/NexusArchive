// Input: Vitest, Testing Library, TablePreviewAction 组件
// Output: TablePreviewAction 组件单元测试
// Pos: src/components/table/__tests__/TablePreviewAction.test.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * TablePreviewAction 组件单元测试
 *
 * 测试覆盖：
 * - 组件渲染
 * - 悬停状态切换
 * - 预览回调触发
 * - 删除按钮显示/隐藏
 * - 自定义预览标签
 * - 事件冒泡阻止
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { TablePreviewAction } from '../TablePreviewAction';

describe('TablePreviewAction', () => {
  // Mock 回调函数
  const mockOnPreview = vi.fn();
  const mockOnDelete = vi.fn();

  beforeEach(() => {
    // 每个测试前清除 mock 调用记录
    mockOnPreview.mockClear();
    mockOnDelete.mockClear();
  });

  describe('基础渲染', () => {
    it('应该成功渲染组件', () => {
      expect(() => (
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
        />
      )).toBeTruthy();
    });

    it('应该渲染预览按钮', () => {
      render(
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
        />
      );

      // 检查是否有"查看"文字
      expect(screen.getByText('查看')).toBeInTheDocument();
    });

    it('应该渲染默认预览标签', () => {
      render(
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
        />
      );

      expect(screen.getByText('查看')).toBeInTheDocument();
    });

    it('应该渲染自定义预览标签', () => {
      render(
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
          previewLabel="预览详情"
        />
      );

      expect(screen.getByText('预览详情')).toBeInTheDocument();
      expect(screen.queryByText('查看')).not.toBeInTheDocument();
    });
  });

  describe('悬停状态', () => {
    it('未悬停时应该显示灰色背景样式', () => {
      const { container } = render(
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
        />
      );

      const button = container.querySelector('button');
      expect(button).toBeInTheDocument();
      expect(button?.className).toContain('bg-gray-100');
      expect(button?.className).toContain('text-gray-600');
    });

    it('悬停时应该显示蓝色背景样式', () => {
      const { container } = render(
        <TablePreviewAction
          hovered={true}
          onPreview={mockOnPreview}
        />
      );

      const button = container.querySelector('button');
      expect(button).toBeInTheDocument();
      expect(button?.className).toContain('bg-blue-600');
      expect(button?.className).toContain('text-white');
      expect(button?.className).toContain('shadow-md');
    });

    it('悬停样式应该包含过渡动画类', () => {
      const { container } = render(
        <TablePreviewAction
          hovered={true}
          onPreview={mockOnPreview}
        />
      );

      const button = container.querySelector('button');
      expect(button?.className).toContain('transition-all');
      expect(button?.className).toContain('duration-200');
    });
  });

  describe('预览功能', () => {
    it('点击预览按钮应该触发 onPreview 回调', () => {
      render(
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
        />
      );

      const previewButton = screen.getByText('查看');
      fireEvent.click(previewButton);

      expect(mockOnPreview).toHaveBeenCalledTimes(1);
    });

    it('应该阻止事件冒泡', () => {
      const mockParentClick = vi.fn();

      const { container } = render(
        <div onClick={mockParentClick}>
          <TablePreviewAction
            hovered={false}
            onPreview={mockOnPreview}
          />
        </div>
      );

      const button = container.querySelector('button');
      fireEvent.click(button!);

      // 预览回调应该被触发
      expect(mockOnPreview).toHaveBeenCalledTimes(1);

      // 父元素的点击不应该被触发（因为 stopPropagation）
      expect(mockParentClick).not.toHaveBeenCalled();
    });

    it('预览按钮应该有正确的 title 属性', () => {
      const { container } = render(
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
        />
      );

      const button = container.querySelector('button');
      expect(button?.getAttribute('title')).toBe('点击预览详情');
    });
  });

  describe('删除功能', () => {
    it('showDelete=false 时不应该显示删除按钮', () => {
      const { container } = render(
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
          showDelete={false}
          onDelete={mockOnDelete}
        />
      );

      const buttons = container.querySelectorAll('button');
      expect(buttons.length).toBe(1); // 只有预览按钮
    });

    it('showDelete=true 且有 onDelete 时应该显示删除按钮', () => {
      const { container } = render(
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
          showDelete={true}
          onDelete={mockOnDelete}
        />
      );

      const buttons = container.querySelectorAll('button');
      expect(buttons.length).toBe(2); // 预览按钮 + 删除按钮
    });

    it('showDelete=true 但没有 onDelete 时不应该显示删除按钮', () => {
      const { container } = render(
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
          showDelete={true}
        />
      );

      const buttons = container.querySelectorAll('button');
      expect(buttons.length).toBe(1); // 只有预览按钮
    });

    it('点击删除按钮应该触发 onDelete 回调', () => {
      render(
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
          showDelete={true}
          onDelete={mockOnDelete}
        />
      );

      const { container } = render(
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
          showDelete={true}
          onDelete={mockOnDelete}
        />
      );

      // 获取第二个按钮（删除按钮）
      const buttons = container.querySelectorAll('button');
      const deleteButton = buttons[1];

      fireEvent.click(deleteButton);

      expect(mockOnDelete).toHaveBeenCalledTimes(1);
    });

    it('删除按钮应该有正确的样式类', () => {
      const { container } = render(
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
          showDelete={true}
          onDelete={mockOnDelete}
        />
      );

      const buttons = container.querySelectorAll('button');
      const deleteButton = buttons[1];

      expect(deleteButton.className).toContain('text-rose-600');
      expect(deleteButton.className).toContain('hover:bg-rose-50');
      expect(deleteButton.className).toContain('rounded-lg');
      expect(deleteButton.className).toContain('transition-colors');
    });

    it('删除按钮应该有正确的 title 属性', () => {
      const { container } = render(
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
          showDelete={true}
          onDelete={mockOnDelete}
        />
      );

      const buttons = container.querySelectorAll('button');
      const deleteButton = buttons[1];

      expect(deleteButton.getAttribute('title')).toBe('删除');
    });
  });

  describe('布局和样式', () => {
    it('应该使用 flex 布局并且右对齐', () => {
      const { container } = render(
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
        />
      );

      const wrapper = container.firstChild as HTMLElement;
      expect(wrapper.className).toContain('flex');
      expect(wrapper.className).toContain('items-center');
      expect(wrapper.className).toContain('justify-end');
      expect(wrapper.className).toContain('gap-2');
    });

    it('预览按钮应该包含图标和文字', () => {
      const { container } = render(
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
        />
      );

      const button = container.querySelector('button');
      expect(button?.className).toContain('flex');
      expect(button?.className).toContain('items-center');
      expect(button?.className).toContain('gap-1.5');
    });
  });

  describe('可访问性', () => {
    it('预览按钮应该是 button 元素', () => {
      const { container } = render(
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
        />
      );

      const button = container.querySelector('button');
      expect(button?.tagName).toBe('BUTTON');
    });

    it('删除按钮应该是 button 元素', () => {
      const { container } = render(
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
          showDelete={true}
          onDelete={mockOnDelete}
        />
      );

      const buttons = container.querySelectorAll('button');
      const deleteButton = buttons[1];
      expect(deleteButton.tagName).toBe('BUTTON');
    });
  });

  describe('边界情况', () => {
    it('应该正确处理 undefined 的 hovered 属性', () => {
      expect(() => {
        render(
          <TablePreviewAction
            hovered={undefined}
            onPreview={mockOnPreview}
          />
        );
      }).toBeTruthy();
    });

    it('应该正确处理空的 previewLabel', () => {
      const { container } = render(
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
          previewLabel=""
        />
      );

      const button = container.querySelector('button');
      expect(button).toBeInTheDocument();
      expect(button?.textContent).toBe('');
    });

    it('应该正确处理特殊字符的 previewLabel', () => {
      const { container } = render(
        <TablePreviewAction
          hovered={false}
          onPreview={mockOnPreview}
          previewLabel="查看详情 & 编辑"
        />
      );

      const button = container.querySelector('button');
      expect(button?.textContent).toBe('查看详情 & 编辑');
    });
  });
});
