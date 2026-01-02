// Input: vitest、@/store/useDrawerStore、@testing-library/react
// Output: 测试用例与断言
// Pos: 前端单元测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, beforeEach } from 'vitest';
import { useDrawerStore } from '../useDrawerStore';

/**
 * useDrawerStore 单元测试
 *
 * 测试覆盖:
 * - 初始化状态
 * - 打开/关闭抽屉
 * - 活动标签页切换
 * - 扩展模式切换
 *
 * @author Agent E - 质量保障工程师
 */
describe('useDrawerStore', () => {
  beforeEach(() => {
    // Reset store before each test
    useDrawerStore.setState({
      isOpen: false,
      activeTab: 'metadata',
      archiveId: null,
      expandedMode: false
    });
  });

  it('should initialize with default state', () => {
    const { result } = renderHook(() => useDrawerStore());
    expect(result.current.isOpen).toBe(false);
    expect(result.current.activeTab).toBe('metadata');
    expect(result.current.archiveId).toBe(null);
    expect(result.current.expandedMode).toBe(false);
  });

  it('should open drawer with archive ID', () => {
    const { result } = renderHook(() => useDrawerStore());
    act(() => {
      result.current.open('archive-123');
    });
    expect(result.current.isOpen).toBe(true);
    expect(result.current.archiveId).toBe('archive-123');
  });

  it('should close drawer and reset state', () => {
    const { result } = renderHook(() => useDrawerStore());
    act(() => {
      result.current.open('archive-123');
      result.current.setActiveTab('voucher');
    });
    expect(result.current.isOpen).toBe(true);
    expect(result.current.activeTab).toBe('voucher');

    act(() => {
      result.current.close();
    });
    expect(result.current.isOpen).toBe(false);
    expect(result.current.archiveId).toBe(null);
    expect(result.current.activeTab).toBe('metadata'); // Reset to default
  });

  it('should set active tab', () => {
    const { result } = renderHook(() => useDrawerStore());
    act(() => {
      result.current.setActiveTab('attachments');
    });
    expect(result.current.activeTab).toBe('attachments');
  });

  it('should toggle expanded mode', () => {
    const { result } = renderHook(() => useDrawerStore());
    act(() => {
      result.current.setExpandedMode(true);
    });
    expect(result.current.expandedMode).toBe(true);
  });
});
