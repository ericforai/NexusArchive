// Input: vitest、testing-library、react-router-dom、本地模块 ArchiveDetailPage、documentsApi、archivesApi
// Output: ArchiveDetailPage 测试用例，覆盖详情页与协作版本标签页
// Pos: src/pages/archives/__tests__/ArchiveDetailPage.test.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { ArchiveDetailPage } from '../ArchiveDetailPage';

const { archivesApiMock, documentsApiMock } = vi.hoisted(() => ({
  archivesApiMock: {
    getArchiveById: vi.fn(async (id: string) => ({
      code: 200,
      data: {
        id,
        archiveCode: `ARCH-${id}`,
        title: '测试档案标题',
      },
    })),
  },
  documentsApiMock: {
    getSection: vi.fn(async () => ({
      data: {
        data: {
          id: 'test-archive-123:main',
          projectId: 'test-archive-123',
          title: '测试档案标题',
          content: '后端已保存内容',
          assignment: {
            id: 'asg-1',
            sectionId: 'test-archive-123:main',
            assigneeId: 'user-2',
            assigneeName: '张三',
            note: '待复核',
            active: true,
          },
          lock: {
            id: 'lock-1',
            sectionId: 'test-archive-123:main',
            lockedBy: 'user-1',
            reason: '编辑中',
            active: true,
          },
          reminders: [
            {
              id: 'rem-0',
              sectionId: 'test-archive-123:main',
              message: '原提醒',
              remindAt: '2026-03-11T10:00:00',
              recipientId: 'user-2',
              recipientName: '张三',
            },
          ],
        },
      },
    })),
    updateSection: vi.fn(async () => ({ data: { data: { id: 'test-archive-123:main' } } })),
    createAssignment: vi.fn(async () => ({ data: { data: { id: 'asg-2' } } })),
    createLock: vi.fn(async () => ({ data: { data: { id: 'lock-2' } } })),
    createReminder: vi.fn(async () => ({ data: { data: { id: 'rem-1' } } })),
    listVersions: vi.fn(async () => ({
      data: {
        data: [
          {
            id: 'ver-1',
            projectId: 'test-archive-123',
            versionName: 'v1',
            description: '首版',
            createdBy: 'user-1',
            createdAt: '2026-03-11T10:00:00',
          },
        ],
      },
    })),
    createVersion: vi.fn(async () => ({ data: { data: { id: 'ver-2' } } })),
    rollbackVersion: vi.fn(async () => ({ data: { data: { id: 'ver-1' } } })),
  },
}));

vi.mock('antd', async () => {
  const React = await import('react');

  const Breadcrumb = ({ items, ...props }: any) => React.createElement(
    'nav',
    { 'data-mock': 'Breadcrumb', ...props },
    ...(items || []).map((item: any) => React.createElement('span', { key: item.title, className: 'breadcrumb-item' }, item.title)),
  );

  const Tabs = ({ items, activeKey, onChange, ...props }: any) => {
    const activeItem = (items || []).find((item: any) => item.key === activeKey) || items?.[0];
    return React.createElement(
      'div',
      { 'data-mock': 'Tabs', ...props },
      React.createElement(
        'div',
        { className: 'tabs-nav' },
        ...(items || []).map((item: any) => React.createElement('button', {
          key: item.key,
          role: 'tab',
          'aria-selected': activeKey === item.key,
          onClick: () => onChange?.(item.key),
        }, item.label)),
      ),
      React.createElement('div', { className: 'tabs-content' }, activeItem?.children),
    );
  };

  const Button = ({ children, onClick, loading: _loading, danger: _danger, type, ...props }: any) => {
    const buttonType = type === 'primary' ? 'button' : (type || 'button');
    return React.createElement('button', { type: buttonType, onClick, ...props }, children);
  };
  const Input = ({ value, onChange, placeholder, ...props }: any) => React.createElement('input', { value, onChange, placeholder, ...props });
  Input.TextArea = ({ value, onChange, placeholder, ...props }: any) => React.createElement('textarea', { value, onChange, placeholder, ...props });
  const Alert = ({ message, description, ...props }: any) => React.createElement('div', { 'data-mock': 'Alert', ...props }, message, description);
  const mockMessage = {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
  };

  return { Alert, Breadcrumb, Button, Input, Tabs, message: mockMessage };
});

vi.mock('../../../api/archives', () => ({
  archivesApi: archivesApiMock,
}));

vi.mock('../../../api/documents', () => ({
  documentsApi: documentsApiMock,
}));

vi.mock('../../../hooks/useFilePreview', () => ({
  useFilePreview: () => ({ previewUrl: null, loading: false })
}));

vi.mock('../hooks/useVoucherData', () => ({
  useVoucherData: () => ({
    voucherData: {
      voucherNo: '记-2024-001',
      voucherWord: '记',
      debitTotal: 10000,
      voucherDate: '2024-01-01',
      attachments: [
        { id: 'att-1', fileId: 'att-1', fileName: 'invoice.ofd', type: 'application/ofd', previewResourceType: 'file' },
      ],
      entries: []
    },
    isLoading: false
  })
}));

vi.mock('../../../components/voucher', async () => {
  const actual = await vi.importActual<typeof import('../../../components/voucher')>('../../../components/voucher');
  return {
    ...actual,
    VoucherMetadata: () => <div>业务元数据内容</div>,
    VoucherPreviewCanvas: () => <div>会计凭证内容</div>,
    OriginalDocumentPreview: ({ files }: { files?: Array<{ previewResourceType?: string; fileId?: string }> }) => (
      <div
        data-testid="original-document-preview"
        data-file-id={files?.[0]?.fileId}
        data-resource-type={files?.[0]?.previewResourceType}
      />
    ),
  };
});

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<any>('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: 'test-archive-123' }),
    useNavigate: () => vi.fn()
  };
});

function renderWithRouter(component: React.ReactElement) {
  return render(
    <BrowserRouter>
      {component}
    </BrowserRouter>
  );
}

describe('ArchiveDetailPage', () => {
  beforeEach(() => {
    Object.values(archivesApiMock).forEach((mockFn) => mockFn.mockClear());
    Object.values(documentsApiMock).forEach((mockFn) => mockFn.mockClear());
  });

  it('should render page with voucher data', () => {
    renderWithRouter(<ArchiveDetailPage />);
    expect(screen.getAllByText('凭证详情')).toHaveLength(2);
  });

  it('should display archive ID from URL params', () => {
    renderWithRouter(<ArchiveDetailPage />);
    const pageElement = document.querySelector('[data-archive-id="test-archive-123"]');
    expect(pageElement).toBeInTheDocument();
  });

  it('should render workflow tab alongside existing tabs', () => {
    renderWithRouter(<ArchiveDetailPage />);
    expect(screen.getByRole('tab', { name: /业务元数据/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /会计凭证/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /关联附件/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /协作与版本/ })).toBeInTheDocument();
  });

  it('should have back button', () => {
    renderWithRouter(<ArchiveDetailPage />);
    const backButton = screen.getByRole('button', { name: /返回/ });
    expect(backButton).toBeInTheDocument();
  });

  it('should display breadcrumb with archive info', () => {
    renderWithRouter(<ArchiveDetailPage />);
    expect(screen.getByText('档案管理')).toBeInTheDocument();
    expect(screen.getAllByText('凭证详情')).toHaveLength(2);
  });

  it('should pass file preview resource metadata into attachment preview entry', () => {
    renderWithRouter(<ArchiveDetailPage />);
    fireEvent.click(screen.getByRole('tab', { name: /关联附件/ }));
    expect(screen.getByTestId('original-document-preview')).toHaveAttribute('data-file-id', 'att-1');
    expect(screen.getByTestId('original-document-preview')).toHaveAttribute('data-resource-type', 'file');
  });

  it('should load persisted collaboration and version data', async () => {
    renderWithRouter(<ArchiveDetailPage />);

    fireEvent.click(screen.getByRole('tab', { name: /协作与版本/ }));

    await waitFor(() => {
      expect(documentsApiMock.getSection).toHaveBeenCalledWith('test-archive-123', 'test-archive-123:main');
    });
    expect(documentsApiMock.listVersions).toHaveBeenCalledWith('test-archive-123');
    expect(await screen.findByDisplayValue('后端已保存内容')).toBeInTheDocument();
    expect(await screen.findByText('v1')).toBeInTheDocument();
  });

  it('should persist assignment through documents api', async () => {
    renderWithRouter(<ArchiveDetailPage />);

    fireEvent.click(screen.getByRole('tab', { name: /协作与版本/ }));

    const assigneeIdInput = (await screen.findAllByPlaceholderText('例如 user-2'))[0];
    fireEvent.change(assigneeIdInput, { target: { value: 'user-9' } });
    fireEvent.click(screen.getByRole('button', { name: '保存分工' }));

    await waitFor(() => {
      expect(documentsApiMock.createAssignment).toHaveBeenCalledWith('test-archive-123', expect.objectContaining({
        sectionId: 'test-archive-123:main',
        assigneeId: 'user-9',
        active: true,
      }));
    });
  });

  it('should persist lock and reminder through documents api', async () => {
    renderWithRouter(<ArchiveDetailPage />);

    fireEvent.click(screen.getByRole('tab', { name: /协作与版本/ }));

    fireEvent.change(await screen.findByDisplayValue('编辑中'), { target: { value: '最终校对中' } });
    fireEvent.click(screen.getByRole('button', { name: '保存锁定' }));

    await waitFor(() => {
      expect(documentsApiMock.createLock).toHaveBeenCalledWith('test-archive-123', expect.objectContaining({
        sectionId: 'test-archive-123:main',
        reason: '最终校对中',
        active: true,
      }));
    });

    fireEvent.change(screen.getByDisplayValue('原提醒'), { target: { value: '请在今晚确认' } });
    fireEvent.change(screen.getAllByPlaceholderText('例如 user-2')[1], { target: { value: 'user-7' } });
    fireEvent.change(screen.getByDisplayValue('2026-03-11T10:00'), { target: { value: '2026-03-11T18:30' } });
    fireEvent.click(screen.getByRole('button', { name: '创建提醒' }));

    await waitFor(() => {
      expect(documentsApiMock.createReminder).toHaveBeenCalledWith('test-archive-123', expect.objectContaining({
        sectionId: 'test-archive-123:main',
        message: '请在今晚确认',
        remindAt: '2026-03-11T18:30',
        recipientId: 'user-7',
      }));
    });
  });

  it('should create version and rollback through documents api', async () => {
    renderWithRouter(<ArchiveDetailPage />);

    fireEvent.click(screen.getByRole('tab', { name: /协作与版本/ }));

    fireEvent.change(await screen.findByPlaceholderText('例如 v1.0'), { target: { value: 'v2' } });
    fireEvent.change(screen.getByPlaceholderText('例如 完成首轮校对'), { target: { value: '新增说明' } });
    fireEvent.click(screen.getByRole('button', { name: '创建版本' }));

    await waitFor(() => {
      expect(documentsApiMock.createVersion).toHaveBeenCalledWith('test-archive-123', {
        versionName: 'v2',
        description: '新增说明',
      });
    });

    fireEvent.click(screen.getByRole('button', { name: '回滚到此版本' }));

    await waitFor(() => {
      expect(documentsApiMock.rollbackVersion).toHaveBeenCalledWith('test-archive-123', 'ver-1');
    });
  });
});
