// Input: React、lucide-react、mfaApi
// Output: MfaVerifyPage 组件
// Pos: MFA 验证页面（登录后验证）
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState } from 'react';
import { Shield, Loader2, CheckCircle2, XCircle, Key, AlertCircle } from 'lucide-react';
import { mfaApi } from '../../api/mfa';
import { useNavigate } from 'react-router-dom';

/**
 * MFA 验证页面
 * 
 * 功能：
 * 1. TOTP 码输入框
 * 2. 备用码输入选项
 * 3. 验证失败提示
 * 
 * PRD 来源: Section 5.3 - 多因素认证
 */
export const MfaVerifyPage: React.FC = () => {
    const navigate = useNavigate();
    const [code, setCode] = useState('');
    const [useBackupCode, setUseBackupCode] = useState(false);
    const [verifying, setVerifying] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleVerify = async () => {
        if (!code.trim()) {
            setError('请输入验证码');
            return;
        }

        setVerifying(true);
        setError(null);

        try {
            const res = useBackupCode
                ? await mfaApi.verifyBackup(code.trim())
                : await mfaApi.verify(code.trim());

            if (res.code === 200) {
                // 验证成功，跳转到首页
                navigate('/system');
            } else {
                setError(res.message || '验证失败，请检查验证码是否正确');
            }
        } catch (error: any) {
            setError(error?.response?.data?.message || '验证失败，请重试');
        } finally {
            setVerifying(false);
        }
    };

    const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter') {
            handleVerify();
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-slate-50 p-4">
            <div className="max-w-md w-full bg-white rounded-lg shadow-lg p-8">
                <div className="text-center mb-8">
                    <div className="mx-auto w-16 h-16 bg-primary-100 rounded-full flex items-center justify-center mb-4">
                        <Shield className="text-primary-600" size={32} />
                    </div>
                    <h1 className="text-2xl font-bold text-slate-800 mb-2">多因素认证</h1>
                    <p className="text-slate-600">
                        请输入您的{useBackupCode ? '备用码' : '验证码'}以继续
                    </p>
                </div>

                {error && (
                    <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
                        <XCircle className="text-red-600 flex-shrink-0 mt-0.5" size={20} />
                        <div className="text-sm text-red-800">{error}</div>
                    </div>
                )}

                <div className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-slate-700 mb-2">
                            {useBackupCode ? '备用码' : '6位验证码'}
                        </label>
                        <div className="relative">
                            <Key className="absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400" size={20} />
                            <input
                                type="text"
                                value={code}
                                onChange={(e) => {
                                    const value = useBackupCode 
                                        ? e.target.value 
                                        : e.target.value.replace(/\D/g, '').slice(0, 6);
                                    setCode(value);
                                    setError(null);
                                }}
                                onKeyPress={handleKeyPress}
                                placeholder={useBackupCode ? '输入备用码' : '000000'}
                                maxLength={useBackupCode ? undefined : 6}
                                className="w-full pl-10 pr-4 py-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 text-center text-xl tracking-widest font-mono"
                                autoFocus
                            />
                        </div>
                        {!useBackupCode && (
                            <p className="text-xs text-slate-500 mt-1">
                                输入验证器应用中显示的6位数字
                            </p>
                        )}
                    </div>

                    <div className="flex items-center">
                        <input
                            type="checkbox"
                            id="useBackupCode"
                            checked={useBackupCode}
                            onChange={(e) => {
                                setUseBackupCode(e.target.checked);
                                setCode('');
                                setError(null);
                            }}
                            className="w-4 h-4 text-primary-600 border-slate-300 rounded focus:ring-primary-500"
                        />
                        <label htmlFor="useBackupCode" className="ml-2 text-sm text-slate-700">
                            使用备用码
                        </label>
                    </div>

                    {useBackupCode && (
                        <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg flex items-start gap-2">
                            <AlertCircle className="text-blue-600 flex-shrink-0 mt-0.5" size={16} />
                            <p className="text-xs text-blue-800">
                                每个备用码只能使用一次。使用后该备用码将失效。
                            </p>
                        </div>
                    )}

                    <button
                        onClick={handleVerify}
                        disabled={verifying || !code.trim() || (!useBackupCode && code.length !== 6)}
                        className="w-full px-4 py-3 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 font-medium"
                    >
                        {verifying ? (
                            <>
                                <Loader2 className="animate-spin" size={20} />
                                验证中...
                            </>
                        ) : (
                            <>
                                <CheckCircle2 size={20} />
                                验证
                            </>
                        )}
                    </button>

                    <div className="text-center">
                        <button
                            onClick={() => navigate(-1)}
                            className="text-sm text-slate-500 hover:text-slate-700"
                        >
                            返回
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default MfaVerifyPage;






