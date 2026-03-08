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

vi.mock('../../preview', () => ({
  SmartFilePreview: ({
    archiveId,
    fileId,
    currentFileId,
    files,
  }: {
    archiveId?: string;
    fileId?: string;
    currentFileId?: string;
    files?: Array<{ fileType?: string }>;
  }) => (
    <div
      data-testid="smart-file-preview"
      data-archive-id={archiveId}
      data-file-id={fileId}
      data-current-file-id={currentFileId}
      data-file-type={files?.[0]?.fileType}
    />
  ),
}));

vi.mock('../../preview/OfdViewer', () => ({
  OfdViewer: ({ fileName }: { fileName?: string }) => (
    <div data-testid="ofd-viewer">ofd:{fileName}</div>
  ),
}));

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

  it('在归档详情单文件非 OFD 场景下应走共享预览链路', () => {
    render(
      <OriginalDocumentPreview
        archiveId="archive-001"
        files={[
          { id: 'pdf-1', fileName: 'invoice.pdf', fileUrl: '/archive/files/download/pdf-1', type: 'application/pdf' },
        ]}
      />,
    );

    const preview = screen.getByTestId('smart-file-preview');

    expect(preview).toHaveAttribute('data-archive-id', 'archive-001');
    expect(preview).toHaveAttribute('data-file-id', 'pdf-1');
    expect(preview).toHaveAttribute('data-current-file-id', 'pdf-1');
    expect(preview).toHaveAttribute('data-file-type', 'pdf');
    expect(client.get).not.toHaveBeenCalled();
  });

  it('在归档详情 OFD 场景下应改走真实文件下载而不是共享预览', async () => {
    vi.mocked(client.get).mockResolvedValue({
      data: new Blob(['ofd'], { type: 'application/ofd' }),
      headers: { 'content-type': 'application/ofd' },
    } as any);

    render(
      <OriginalDocumentPreview
        archiveId="archive-001"
        files={[
          { id: 'ofd-1', fileName: 'invoice.ofd', fileUrl: '/archive/files/download/ofd-1', type: 'application/ofd' },
        ]}
      />,
    );

    expect(screen.queryByTestId('smart-file-preview')).not.toBeInTheDocument();
    expect(await screen.findByTestId('ofd-viewer')).toBeInTheDocument();
    expect(client.get).toHaveBeenCalledWith('/archive/files/download/ofd-1', { responseType: 'blob' });
  });
});
