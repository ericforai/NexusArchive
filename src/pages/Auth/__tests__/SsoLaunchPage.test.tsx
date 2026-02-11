import React from 'react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import SsoLaunchPage from '../SsoLaunchPage';
import { ssoApi } from '../../../api/sso';

const loginMock = vi.fn();

vi.mock('../../../store/useAuthStore', () => ({
  useAuthStore: (selector: any) => selector({ login: loginMock }),
}));

vi.mock('../../../api/sso', () => ({
  ssoApi: {
    consume: vi.fn(),
  },
}));

const RedirectProbe = () => {
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  return <div data-testid="redirect-probe">{params.get('voucherNo')}|{params.get('autoSearch')}</div>;
};

describe('SsoLaunchPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should consume ticket, login and redirect with voucherNo', async () => {
    vi.mocked(ssoApi.consume).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: {
        token: 'jwt-token',
        user: {
          id: 'u1',
          username: 'zhangsan',
          roles: [],
          permissions: [],
        },
        voucherNo: '记-8',
      },
    } as any);

    render(
      <MemoryRouter initialEntries={['/system/sso/launch?ticket=t1']}>
        <Routes>
          <Route path="/system/sso/launch" element={<SsoLaunchPage />} />
          <Route path="/system/utilization/relationship" element={<RedirectProbe />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(ssoApi.consume).toHaveBeenCalledWith('t1');
      expect(loginMock).toHaveBeenCalled();
      expect(screen.getByTestId('redirect-probe').textContent).toBe('记-8|1');
    });
  });

  it('should show error when ticket missing', async () => {
    render(
      <MemoryRouter initialEntries={['/system/sso/launch']}>
        <Routes>
          <Route path="/system/sso/launch" element={<SsoLaunchPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('单点登录失败')).toBeInTheDocument();
      expect(screen.getByText('缺少 ticket 参数')).toBeInTheDocument();
    });
  });
});
