// Input: vitest、testing-library、SmartFilePreview
// Output: SmartFilePreview OFD 分流测试
// Pos: 预览组件测试

import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { SmartFilePreview } from '../SmartFilePreview';

vi.mock('../useFilePreview', () => ({
  useFilePreview: () => ({
    blobUrl: 'blob:ofd-preview',
    presignedUrl: null,
    loading: false,
    error: null,
    retry: vi.fn(),
  }),
}));

vi.mock('../OfdViewer', () => ({
  OfdViewer: () => <div data-testid="ofd-viewer" />,
}));

describe('SmartFilePreview', () => {
  it('renders the OFD viewer for ofd files', () => {
    render(
      <SmartFilePreview
        isPool={true}
        fileId="file-ofd"
        fileName="invoice.ofd"
        files={[{ id: 'file-ofd', fileName: 'invoice.ofd', fileType: 'ofd' }]}
        currentFileId="file-ofd"
      />,
    );

    expect(screen.getByTestId('ofd-viewer')).toBeInTheDocument();
  });
});
