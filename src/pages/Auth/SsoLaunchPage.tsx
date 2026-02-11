// Input: React Router、SSO API、AuthStore
// Output: SsoLaunchPage
// Pos: SSO 落地页

import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ssoApi } from '../../api/sso';
import { useAuthStore } from '../../store/useAuthStore';

const SsoLaunchPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const login = useAuthStore((s) => s.login);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const ticket = searchParams.get('ticket');
    if (!ticket) {
      setError('缺少 ticket 参数');
      return;
    }

    let cancelled = false;
    (async () => {
      try {
        const res = await ssoApi.consume(ticket);
        if (cancelled) return;
        if (res.code !== 200 || !res.data) {
          setError(res.message || 'SSO 登录失败');
          return;
        }

        login(res.data.token, {
          id: res.data.user.id,
          username: res.data.user.username,
          realName: res.data.user.fullName,
          fullName: res.data.user.fullName,
          email: res.data.user.email,
          avatar: res.data.user.avatar,
          departmentId: res.data.user.departmentId,
          status: res.data.user.status,
          roles: res.data.user.roles || [],
          permissions: res.data.user.permissions || [],
          phone: res.data.user.phone,
          employeeId: res.data.user.employeeId,
          jobTitle: res.data.user.jobTitle,
          orgCode: res.data.user.orgCode,
          lastLoginAt: res.data.user.lastLoginAt,
          createdTime: res.data.user.createdTime,
          roleNames: res.data.user.roleNames,
          allowedFonds: res.data.user.allowedFonds,
        });

        const params = new URLSearchParams();
        params.set('voucherNo', res.data.voucherNo);
        params.set('autoSearch', '1');
        navigate(`/system/utilization/relationship?${params.toString()}`, { replace: true });
      } catch (e: any) {
        if (!cancelled) {
          setError(e?.message || 'SSO 登录异常');
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [login, navigate, searchParams]);

  return (
    <div className="flex items-center justify-center h-screen text-slate-700">
      <div className="text-center">
        {error ? (
          <>
            <h2 className="text-lg font-semibold mb-2">单点登录失败</h2>
            <p className="text-sm text-rose-600">{error}</p>
          </>
        ) : (
          <>
            <h2 className="text-lg font-semibold mb-2">正在进入联查系统</h2>
            <p className="text-sm text-slate-500">请稍候...</p>
          </>
        )}
      </div>
    </div>
  );
};

export default SsoLaunchPage;
