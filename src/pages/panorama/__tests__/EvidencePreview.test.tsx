// Input: vitest、testing-library、EvidencePreview
// Output: 全景证据预览 OFD 接入测试
// Pos: Panorama 页面测试

import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { EvidencePreview } from '../EvidencePreview';

const { getByArchive, getOriginalVoucherFiles } = vi.hoisted(() => ({
  getByArchive: vi.fn(),
  getOriginalVoucherFiles: vi.fn(),
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

vi.mock('@/store', () => ({
  useAuthStore: (selector: (state: { token: string }) => string) => selector({ token: 'token-123' }),
}));

vi.mock('@/components/preview', () => ({
  UnifiedOfdPreview: ({ fileId, fileName, sourceType }: { fileId?: string; fileName?: string; sourceType?: string | null }) => (
    <div data-testid="unified-ofd-preview">ofd:{fileId}:{fileName}:{sourceType}</div>
  ),
}));

describe('EvidencePreview', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('routes archive ofd files to the unified preview decision layer', async () => {
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
    expect(await screen.findByTestId('unified-ofd-preview')).toHaveTextContent('ofd:file-ofd:invoice.ofd:ARCHIVE');
  });

  it('routes original voucher ofd files to the unified preview decision layer', async () => {
    getByArchive.mockResolvedValue([]);
    getOriginalVoucherFiles.mockResolvedValue([
      {
        id: 'orig-ofd',
        voucherId: 'voucher-1',
        fileName: 'original-invoice.ofd',
        fileType: 'ofd',
        fileSize: 1024,
        storagePath: '/tmp/original-invoice.ofd',
        fileRole: 'ORIGINAL',
      },
    ]);

    render(<EvidencePreview voucherId="voucher-1" sourceType="ORIGINAL" />);

    await screen.findByText('original-invoice.ofd');
    expect(await screen.findByTestId('unified-ofd-preview')).toHaveTextContent('ofd:orig-ofd:original-invoice.ofd:ORIGINAL');
  });
});
