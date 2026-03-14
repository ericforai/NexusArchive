import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { UnifiedOfdPreview } from '../UnifiedOfdPreview';

const { getResource } = vi.hoisted(() => ({
  getResource: vi.fn(),
}));

vi.mock('@/api/ofdPreview', () => ({
  ofdPreviewApi: {
    getResource,
  },
}));

vi.mock('../LiteOfdPreview', () => ({
  LiteOfdPreview: ({ fileName, downloadUrl }: { fileName?: string; downloadUrl?: string }) => (
    <div data-testid="liteofd-preview">
      liteofd:{fileName}:{downloadUrl}
    </div>
  ),
}));

describe('UnifiedOfdPreview', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('prefers converted pdf preview when backend returns one', async () => {
    getResource.mockResolvedValue({
      preferredMode: 'converted',
      fileName: 'invoice.ofd',
      convertedFileId: 'pdf-file',
      convertedMimeType: 'application/pdf',
      convertedPreviewUrl: '/api/preview?resourceType=file&fileId=pdf-file&mode=stream',
      originalFileId: 'orig-ofd',
      originalDownloadUrl: '/api/original-vouchers/files/download/orig-ofd',
    });

    render(<UnifiedOfdPreview fileId="orig-ofd" fileName="invoice.ofd" sourceType="ORIGINAL" originalDownloadUrl="/api/original-vouchers/files/download/orig-ofd" />);

    const frame = await screen.findByTitle('OFD Converted Preview');
    expect(frame).toHaveAttribute('src', '/api/preview?resourceType=file&fileId=pdf-file&mode=stream');
    expect(screen.getByRole('link', { name: '下载原始 OFD' })).toHaveAttribute(
      'href',
      '/api/original-vouchers/files/download/orig-ofd',
    );
  });

  it('falls back to liteofd when converted artifact is unavailable', async () => {
    getResource.mockResolvedValue({
      preferredMode: 'liteofd',
      fileName: 'invoice.ofd',
      convertedFileId: null,
      convertedMimeType: null,
      convertedPreviewUrl: null,
      originalFileId: 'orig-ofd',
      originalDownloadUrl: '/api/original-vouchers/files/download/orig-ofd',
    });

    render(<UnifiedOfdPreview fileId="orig-ofd" fileName="invoice.ofd" sourceType="ORIGINAL" originalDownloadUrl="/api/original-vouchers/files/download/orig-ofd" />);

    expect(await screen.findByTestId('liteofd-preview')).toHaveTextContent(
      'liteofd:invoice.ofd:/api/original-vouchers/files/download/orig-ofd',
    );
  });

  it('keeps the raw download when liteofd preview also fails', async () => {
    getResource.mockRejectedValue(new Error('preview lookup failed'));

    render(<UnifiedOfdPreview fileId="orig-ofd" fileName="invoice.ofd" sourceType="ORIGINAL" originalDownloadUrl="/api/original-vouchers/files/download/orig-ofd" />);

    expect(await screen.findByText('preview lookup failed')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '下载原始 OFD' })).toHaveAttribute(
      'href',
      '/api/original-vouchers/files/download/orig-ofd',
    );
  });

  it('falls back to liteofd when the preview resource endpoint is missing', async () => {
    getResource.mockRejectedValue({
      response: {
        status: 404,
      },
      message: 'Request failed with status code 404',
    });

    render(<UnifiedOfdPreview fileId="orig-ofd" fileName="invoice.ofd" sourceType="ORIGINAL" originalDownloadUrl="/api/original-vouchers/files/download/orig-ofd" />);

    expect(await screen.findByTestId('liteofd-preview')).toHaveTextContent(
      'liteofd:invoice.ofd:/api/original-vouchers/files/download/orig-ofd',
    );
  });

  it('can render liteofd directly from an explicit original download url', async () => {
    getResource.mockResolvedValue({
      preferredMode: 'liteofd',
      fileName: 'invoice.ofd',
      convertedFileId: null,
      convertedMimeType: null,
      convertedPreviewUrl: null,
      originalFileId: '',
      originalDownloadUrl: '',
    });

    render(
      <UnifiedOfdPreview
        fileName="invoice.ofd"
        sourceType="ORIGINAL"
        originalDownloadUrl="/api/original-vouchers/files/download/orig-ofd"
      />,
    );

    expect(await screen.findByTestId('liteofd-preview')).toHaveTextContent(
      'liteofd:invoice.ofd:/api/original-vouchers/files/download/orig-ofd',
    );
  });
});
