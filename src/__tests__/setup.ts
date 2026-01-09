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

    return {
        message: mockMessage,
        // Mock commonly used antd components
        Button: createComponentMock('Button'),
        Table: createComponentMock('Table'),
        Drawer: createComponentMock('Drawer'),
        Modal: createComponentMock('Modal'),
        Form: createComponentMock('Form'),
        Input: createComponentMock('Input'),
        Select: createComponentMock('Select'),
        DatePicker: createComponentMock('DatePicker'),
        Space: createComponentMock('Space'),
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
        Card: createComponentMock('Card'),
        Row: createComponentMock('Row'),
        Col: createComponentMock('Col'),
        Alert: createComponentMock('Alert'),
        Collapse: createComponentMock('Collapse'),
        Descriptions: createComponentMock('Descriptions'),
        Progress: createComponentMock('Progress'),
        Typography: createComponentMock('Typography'),
        ConfigProvider: createComponentMock('ConfigProvider'),
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
}));
