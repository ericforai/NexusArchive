// Input: @testing-library/jest-dom/vitest
// Output: 测试环境初始化逻辑
// Pos: 前端单元测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/// <reference types="vitest/globals" />
import '@testing-library/jest-dom/vitest';
import { vi } from 'vitest';

// Mock localStorage
const localStorageMock = {
    getItem: vi.fn(),
    setItem: vi.fn(),
    removeItem: vi.fn(),
    clear: vi.fn(),
    length: 0,
    key: vi.fn(),
};

Object.defineProperty(window, 'localStorage', {
    value: localStorageMock,
});

// Mock window.matchMedia (required for useThemeStore)
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(), // deprecated
        removeListener: vi.fn(), // deprecated
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
    })),
});

// Mock ResizeObserver (required for Ant Design components)
global.ResizeObserver = class ResizeObserver {
    observe() { }
    unobserve() { }
    disconnect() { }
};

// Mock console.log/warn to reduce noise in tests
vi.spyOn(console, 'log').mockImplementation(() => { });
vi.spyOn(console, 'warn').mockImplementation(() => { });

// Mock react-hot-toast
vi.mock('react-hot-toast', () => ({
    toast: {
        success: vi.fn(),
        error: vi.fn(),
        loading: vi.fn(),
        dismiss: vi.fn(),
    },
    default: {
        success: vi.fn(),
        error: vi.fn(),
        loading: vi.fn(),
        dismiss: vi.fn(),
    },
}));

// Mock Ant Design message component (required for notificationService)
// This mock must be defined before any component that imports notificationService
vi.mock('antd', () => {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const React = require('react');
    const mockMessage = {
        success: vi.fn(() => mockMessage),
        error: vi.fn(() => mockMessage),
        warning: vi.fn(() => mockMessage),
        info: vi.fn(() => mockMessage),
        loading: vi.fn(() => mockMessage),
        destroy: vi.fn(),
    };

    // Create functional component mocks for antd components
    const createComponentMock = (name: string) => {
        const Component = ({ children, ...props }: any) => {
            return React.createElement('div', { 'data-mock': name, ...props }, children);
        };
        Component.displayName = name;
        return Component;
    };

    // Card mock that renders title as text content
    const CardMock = ({ children, title, ...props }: any) => {
        return React.createElement('div', { 'data-mock': 'Card', ...props },
            title ? React.createElement('div', { className: 'card-title' }, title) : null,
            children
        );
    };

    // Modal mock that renders title as text content
    const ModalMock = ({ children, title, ...props }: any) => {
        return React.createElement('div', { 'data-mock': 'Modal', ...props },
            title ? React.createElement('div', { className: 'modal-title' }, title) : null,
            children
        );
    };

    // Drawer mock that renders title as text content
    const DrawerMock = ({ children, title, ...props }: any) => {
        return React.createElement('div', { 'data-mock': 'Drawer', ...props },
            title ? React.createElement('div', { className: 'drawer-title' }, title) : null,
            children
        );
    };

    // Alert mock that renders message and description as text content
    const AlertMock = ({ message, description, children, ...props }: any) => {
        return React.createElement('div', { 'data-mock': 'Alert', ...props },
            message ? React.createElement('div', { className: 'alert-message' }, message) : null,
            description ? React.createElement('div', { className: 'alert-description' }, description) : null,
            children
        );
    };

    // Segmented mock that renders options as text content
    const SegmentedMock = ({ options, value, onChange, ...props }: any) => {
        const optionsArray = Array.isArray(options) ? options : [];
        return React.createElement('div', { 'data-mock': 'Segmented', ...props },
            ...optionsArray.map((opt: any) => {
                const label = typeof opt === 'string' ? opt : opt.label;
                return React.createElement('span', {
                    className: `segmented-option ${value === label ? 'selected' : ''}`,
                    onClick: () => onChange?.(label)
                }, label);
            })
        );
    };

    // Breadcrumb mock that renders items as text content
    const _BreadcrumbMock = ({ items, ...props }: any) => {
        return React.createElement('nav', { 'data-mock': 'Breadcrumb', ...props },
            ...items.map((item: any) =>
                React.createElement('span', { key: item.title, className: 'breadcrumb-item' }, item.title)
            )
        );
    };

    // Tabs mock that renders tab items with proper role
    const TabsMock = ({ items, activeKey, onChange, ...props }: any) => {
        const itemsArray = items || [];
        return React.createElement('div', { 'data-mock': 'Tabs', ...props },
            React.createElement('div', { className: 'tabs-nav' },
                ...itemsArray.map((item: any) =>
                    React.createElement('button', {
                        key: item.key,
                        role: 'tab',
                        'aria-selected': activeKey === item.key,
                        onClick: () => onChange?.(item.key)
                    }, item.label)
                )
            )
        );
    };

    const FormMock: any = createComponentMock('Form');
    FormMock.Item = createComponentMock('Form.Item');

    const InputMock: any = ({ children, ...props }: any) => {
        return React.createElement('input', { 'data-mock': 'Input', ...props }, children);
    };
    InputMock.displayName = 'Input';

    InputMock.TextArea = ({ children, ...props }: any) => {
        return React.createElement('textarea', { 'data-mock': 'Input.TextArea', ...props }, children);
    };
    InputMock.TextArea.displayName = 'Input.TextArea';

    InputMock.Password = ({ children, ...props }: any) => {
        return React.createElement('input', { 'data-mock': 'Input.Password', type: 'password', ...props }, children);
    };
    InputMock.Password.displayName = 'Input.Password';

    const SelectMock: any = createComponentMock('Select');
    SelectMock.Option = createComponentMock('Select.Option');

    const SpaceMock: any = createComponentMock('Space');
    SpaceMock.Compact = createComponentMock('Space.Compact');

    const DatePickerMock: any = createComponentMock('DatePicker');
    DatePickerMock.RangePicker = createComponentMock('RangePicker');

    return {
        message: mockMessage,
        // Mock commonly used antd components
        Button: createComponentMock('Button'),
        Table: createComponentMock('Table'),
        Drawer: DrawerMock,
        Modal: ModalMock,
        Form: FormMock,
        Input: InputMock,
        Select: SelectMock,
        DatePicker: DatePickerMock,
        Space: SpaceMock,
        Divider: createComponentMock('Divider'),
        Tag: createComponentMock('Tag'),
        Badge: createComponentMock('Badge'),
        Tooltip: createComponentMock('Tooltip'),
        Popconfirm: createComponentMock('Popconfirm'),
        Dropdown: createComponentMock('Dropdown'),
        Menu: createComponentMock('Menu'),
        Spin: createComponentMock('Spin'),
        Empty: createComponentMock('Empty'),
        Pagination: createComponentMock('Pagination'),
        Card: CardMock,
        Row: createComponentMock('Row'),
        Col: createComponentMock('Col'),
        Alert: AlertMock,
        Breadcrumb: _BreadcrumbMock,
        Collapse: createComponentMock('Collapse'),
        Descriptions: createComponentMock('Descriptions'),
        Progress: createComponentMock('Progress'),
        Typography: createComponentMock('Typography'),
        ConfigProvider: createComponentMock('ConfigProvider'),
        Tabs: TabsMock,
        Segmented: SegmentedMock,
        Switch: createComponentMock('Switch'),
        TextArea: InputMock.TextArea, // Alias for Input.TextArea
        RangePicker: DatePickerMock.RangePicker, // Alias for DatePicker.RangePicker
    };
});

// Mock @ant-design/icons
vi.mock('@ant-design/icons', () => ({
    DownOutlined: () => null,
    UpOutlined: () => null,
    CheckOutlined: () => null,
    CloseOutlined: () => null,
    InfoCircleOutlined: () => null,
    WarningOutlined: () => null,
    CheckCircleOutlined: () => null,
    CloseCircleOutlined: () => null,
    ExclamationCircleOutlined: () => null,
    LoadingOutlined: () => null,
    PlusOutlined: () => null,
    DeleteOutlined: () => null,
    EditOutlined: () => null,
    SearchOutlined: () => null,
    SyncOutlined: () => null,
    DownloadOutlined: () => null,
    UploadOutlined: () => null,
    FileOutlined: () => null,
    FolderOutlined: () => null,
    HomeOutlined: () => null,
    SettingOutlined: () => null,
    UserOutlined: () => null,
    FileTextOutlined: () => null,
    FileExcelOutlined: () => null,
    FilePdfOutlined: () => null,
    ReloadOutlined: () => null,
    PlayCircleOutlined: () => null,
    EyeOutlined: () => null,
    PlusCircleOutlined: () => null,
    SafetyCertificateOutlined: () => null,
}));
