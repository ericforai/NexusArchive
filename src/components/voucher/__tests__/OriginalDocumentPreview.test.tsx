import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { OriginalDocumentPreview } from '../OriginalDocumentPreview';
import { client } from '../../../api/client';

vi.mock('../../../api/client', () => ({
  client: {
    get: vi.fn(),
  },
}));

vi.mock('antd', async () => {
  const actual = await vi.importActual<typeof import('antd')>('antd');
  return {
    ...actual,
    message: {
      error: vi.fn(),
    },
  };
});

describe('OriginalDocumentPreview', () => {
  const createObjectURLSpy = vi.fn(() => 'blob:mock-url');
  const revokeObjectURLSpy = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal('URL', {
      createObjectURL: createObjectURLSpy,
      revokeObjectURL: revokeObjectURLSpy,
    });

    vi.mocked(client.get).mockResolvedValue({
      data: new Blob(['test'], { type: 'application/pdf' }),
      headers: { 'content-type': 'application/pdf' },
    } as any);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('切换到新节点文件列表时应重置选中文件并加载预览', async () => {
    const filesA = [
      { id: 'f1', fileName: 'a.pdf', fileUrl: '/archive/files/download/f1', type: 'application/pdf' },
      { id: 'f2', fileName: 'b.pdf', fileUrl: '/archive/files/download/f2', type: 'application/pdf' },
    ];
    const filesB = [
      { id: 'f3', fileName: 'c.pdf', fileUrl: '/archive/files/download/f3', type: 'application/pdf' },
    ];

    const { rerender } = render(<OriginalDocumentPreview files={filesA} defaultFileIndex={1} />);

    await waitFor(() => {
      expect(screen.getByTitle('b.pdf')).toBeInTheDocument();
    });

    rerender(<OriginalDocumentPreview files={filesB} defaultFileIndex={0} />);

    await waitFor(() => {
      expect(screen.getByTitle('c.pdf')).toBeInTheDocument();
    });

    expect(client.get).toHaveBeenCalledWith('/archive/files/download/f3', { responseType: 'blob' });
  });
});
