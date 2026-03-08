// Input: vitest、testing-library、EvidencePreview
// Output: 全景证据预览 OFD 接入测试
// Pos: Panorama 页面测试

import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { EvidencePreview } from '../EvidencePreview';

const { getByArchive, getOriginalVoucherFiles, get } = vi.hoisted(() => ({
  getByArchive: vi.fn(),
  getOriginalVoucherFiles: vi.fn(),
  get: vi.fn(),
}));

vi.mock('@/api/attachments', () => ({
  attachmentsApi: {
    getByArchive,
  },
}));

vi.mock('@/api/originalVoucher', () => ({
  originalVoucherApi: {
    getOriginalVoucherFiles,
  },
}));

vi.mock('@/api/client', () => ({
  client: {
    get,
  },
}));

vi.mock('@/store', () => ({
  useAuthStore: (selector: (state: { token: string }) => string) => selector({ token: 'token-123' }),
}));

vi.mock('@/components/preview/OfdViewer', () => ({
  OfdViewer: ({ fileName }: { fileName?: string }) => (
    <div data-testid="ofd-viewer">ofd:{fileName}</div>
  ),
}));

describe('EvidencePreview', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:ofd-preview'),
      revokeObjectURL: vi.fn(),
    });
    get.mockResolvedValue({
      data: new Blob(['ofd'], { type: 'application/ofd' }),
    });
  });

  it('loads archive ofd through the authenticated download flow', async () => {
    getByArchive.mockResolvedValue([
      {
        id: 'file-ofd',
        archivalCode: 'ARCH-1',
        fileName: 'invoice.ofd',
        fileType: 'ofd',
        fileSize: 2048,
        fileHash: 'hash',
        storagePath: '/tmp/invoice.ofd',
        docType: 'invoice',
      },
    ]);
    getOriginalVoucherFiles.mockResolvedValue([]);

    render(<EvidencePreview voucherId="archive-1" sourceType="ARCHIVE" />);

    await screen.findByText('invoice.ofd');
    expect(await screen.findByTestId('ofd-viewer')).toBeInTheDocument();
    expect(get).toHaveBeenCalledWith('/api/archive/files/download/file-ofd', { responseType: 'blob' });
  });
});
