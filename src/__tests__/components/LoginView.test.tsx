// Input: vitest、@testing-library/react、@testing-library/user-event、react-router-dom 路由、@/pages/Auth/LoginView、@/store/useAuthStore、@/api/auth
// Output: 测试用例与断言
// Pos: 前端单元测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { describe, it, expect, beforeEach, vi, Mock } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { LoginView } from '@/pages/Auth/LoginView';
import { useAuthStore } from '@/store/useAuthStore';
import { authApi } from '@/api/auth';

// Mock API
vi.mock('@/api/auth', () => ({
    authApi: {
        login: vi.fn(),
    },
}));

// Mock navigation
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
        useLocation: () => ({ state: null }),
    };
});

// Mock triggerAuditRefresh
vi.mock('@/utils/audit', () => ({
    triggerAuditRefresh: vi.fn(),
}));

/**
 * LoginView 组件测试
 * 
 * 测试覆盖:
 * - 表单渲染
 * - 表单输入
 * - 登录成功流程
 * - 登录失败处理
 * 
 * @author Agent E - 质量保障工程师
 */
describe('LoginView', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        // 重置 auth store
        useAuthStore.getState().logout();
    });

    const renderLoginView = () => {
        return render(
            <BrowserRouter>
                <LoginView />
            </BrowserRouter>
        );
    };

    // 获取表单元素的辅助函数
    const getFormElements = () => {
        // 使用 role 选择器，因为原始 label 没有 for 属性
        const usernameInput = screen.getByRole('textbox'); // 只有一个 text input
        const passwordInput = document.querySelector('input[type="password"]') as HTMLInputElement;
        const submitButton = screen.getByRole('button', { name: /^登录$/ });
        return { usernameInput, passwordInput, submitButton };
    };

    describe('渲染', () => {
        it('应显示登录表单标题', () => {
            renderLoginView();

            expect(screen.getByRole('heading', { name: '登录' })).toBeInTheDocument();
        });

        it('应包含用户名和密码输入框', () => {
            renderLoginView();

            const { usernameInput, passwordInput } = getFormElements();
            expect(usernameInput).toBeInTheDocument();
            expect(passwordInput).toBeInTheDocument();
        });

        it('应显示登录按钮', () => {
            renderLoginView();

            expect(screen.getByRole('button', { name: /^登录$/ })).toBeInTheDocument();
        });

        it('应显示访问产品官网按钮', () => {
            renderLoginView();

            expect(screen.getByRole('button', { name: /访问产品官网/ })).toBeInTheDocument();
        });
    });

    describe('表单输入', () => {
        it('应能输入用户名和密码', async () => {
            renderLoginView();
            const user = userEvent.setup();

            const { usernameInput, passwordInput } = getFormElements();

            await user.type(usernameInput, 'admin');
            await user.type(passwordInput, 'password123');

            expect(usernameInput).toHaveValue('admin');
            expect(passwordInput).toHaveValue('password123');
        });
    });

    describe('登录成功', () => {
        it('登录成功应调用 AuthStore.login 并导航', async () => {
            const mockLoginResponse = {
                code: 200,
                data: {
                    token: 'jwt-token-123',
                    user: {
                        id: 'user-001',
                        username: 'admin',
                        fullName: '管理员',
                        roles: ['ADMIN'],
                        permissions: ['archive:read'],
                    },
                },
            };

            (authApi.login as Mock).mockResolvedValue(mockLoginResponse);

            renderLoginView();
            const user = userEvent.setup();
            const { usernameInput, passwordInput, submitButton } = getFormElements();

            await user.type(usernameInput, 'admin');
            await user.type(passwordInput, 'admin123');
            await user.click(submitButton);

            await waitFor(() => {
                expect(authApi.login).toHaveBeenCalledWith({
                    username: 'admin',
                    password: 'admin123',
                });
            });

            await waitFor(() => {
                const state = useAuthStore.getState();
                expect(state.isAuthenticated).toBe(true);
                expect(state.token).toBe('jwt-token-123');
            });

            expect(mockNavigate).toHaveBeenCalledWith('/system', { replace: true });
        });
    });

    describe('登录失败', () => {
        it('登录失败应显示错误信息', async () => {
            const mockErrorResponse = {
                code: 401,
                message: '用户名或密码错误',
            };

            (authApi.login as Mock).mockResolvedValue(mockErrorResponse);

            renderLoginView();
            const user = userEvent.setup();
            const { usernameInput, passwordInput, submitButton } = getFormElements();

            await user.type(usernameInput, 'admin');
            await user.type(passwordInput, 'wrongpassword');
            await user.click(submitButton);

            await waitFor(() => {
                expect(screen.getByText('用户名或密码错误')).toBeInTheDocument();
            });
        });

        it('网络错误应显示网络错误提示', async () => {
            (authApi.login as Mock).mockRejectedValue(new Error('Network Error'));

            renderLoginView();
            const user = userEvent.setup();
            const { usernameInput, passwordInput, submitButton } = getFormElements();

            await user.type(usernameInput, 'admin');
            await user.type(passwordInput, 'admin123');
            await user.click(submitButton);

            await waitFor(() => {
                expect(screen.getByText('网络错误')).toBeInTheDocument();
            });
        });
    });

    describe('加载状态', () => {
        it('登录中应显示加载状态', async () => {
            // 创建一个永不 resolve 的 promise 来保持加载状态
            (authApi.login as Mock).mockImplementation(() => new Promise(() => { }));

            renderLoginView();
            const user = userEvent.setup();
            const { usernameInput, passwordInput, submitButton } = getFormElements();

            await user.type(usernameInput, 'admin');
            await user.type(passwordInput, 'admin123');
            await user.click(submitButton);

            await waitFor(() => {
                expect(screen.getByRole('button', { name: /登录中/ })).toBeInTheDocument();
                expect(screen.getByRole('button', { name: /登录中/ })).toBeDisabled();
            });
        });
    });

    describe('访问产品官网', () => {
        it('点击访问产品官网应导航到首页', async () => {
            renderLoginView();
            const user = userEvent.setup();

            await user.click(screen.getByRole('button', { name: /访问产品官网/ }));

            expect(mockNavigate).toHaveBeenCalledWith('/');
        });

        it('提供 onVisitLanding 回调时应调用回调', async () => {
            const mockCallback = vi.fn();

            render(
                <BrowserRouter>
                    <LoginView onVisitLanding={mockCallback} />
                </BrowserRouter>
            );
            const user = userEvent.setup();

            await user.click(screen.getByRole('button', { name: /访问产品官网/ }));

            expect(mockCallback).toHaveBeenCalled();
        });
    });
});
