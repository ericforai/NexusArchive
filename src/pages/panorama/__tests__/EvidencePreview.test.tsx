// Input: vitest、testing-library、EvidencePreview
// Output: 全景证据预览 OFD 接入测试
// Pos: Panorama 页面测试

import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
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
  SmartFilePreview: ({ fileId }: { fileId?: string }) => (
    <div data-testid="smart-file-preview">preview:{fileId}</div>
  ),
}));

describe('EvidencePreview', () => {
  it('routes selected ofd attachment to the shared preview flow', async () => {
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
    expect(screen.getByTestId('smart-file-preview')).toBeInTheDocument();
  });
});
