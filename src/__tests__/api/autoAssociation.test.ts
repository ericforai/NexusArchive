import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../../store', () => ({
  useAuthStore: {
    getState: vi.fn(),
    subscribe: vi.fn(),
  },
  useFondsStore: {
    getState: vi.fn(),
    subscribe: vi.fn(),
  },
}));

import { client } from '../../api/client';
import { autoAssociationApi } from '../../api/autoAssociation';
import { useAuthStore, useFondsStore } from '../../store';

describe('autoAssociationApi.getLinkedFiles', () => {
  let originalAdapter: any;

  beforeEach(() => {
    originalAdapter = client.defaults.adapter;
    (useAuthStore.getState as any).mockReturnValue({
      token: 'test-token',
      logout: vi.fn(),
    });
    (useFondsStore.getState as any).mockReturnValue({
      getCurrentFondsCode: () => 'F001',
    });
  });

  afterEach(() => {
    client.defaults.adapter = originalAdapter;
    vi.clearAllMocks();
  });

  it('真实档号无关联附件时不应回退到 demo 文件', async () => {
    client.defaults.adapter = async (config) => ({
      data: {
        code: 200,
        data: [],
      },
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    });

    const result = await autoAssociationApi.getLinkedFiles('seed-contract-001');

    expect(result.files).toEqual([]);
  });

  it('demo 查询无后端数据时仍应返回 demo 文件', async () => {
    client.defaults.adapter = async (config) => ({
      data: {
        code: 200,
        data: [],
      },
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    });

    const result = await autoAssociationApi.getLinkedFiles('demo');

    expect(result.files).toHaveLength(1);
    expect(result.files[0]?.id).toBe('demo-file-voucher-001');
  });
});
