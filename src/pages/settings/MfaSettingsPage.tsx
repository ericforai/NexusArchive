// Input: React、lucide-react、mfaApi
// Output: MfaSettingsPage 组件
// Pos: MFA 设置页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { Shield, Loader2, CheckCircle2, XCircle, Copy, Download, Printer, AlertCircle, Eye, EyeOff } from 'lucide-react';
import { mfaApi, MfaStatus, MfaSetupResponse } from '../../api/mfa';

/**
 * MFA 设置页面
 * 
 * 功能：
 * 1. 启用/禁用 MFA
 * 2. TOTP 二维码展示
 * 3. 备用码生成和展示
 * 4. 备用码下载/打印
 * 
 * PRD 来源: Section 5.3 - 多因素认证
 */
export const MfaSettingsPage: React.FC = () => {
    const [status, setStatus] = useState<MfaStatus | null>(null);
    const [setupData, setSetupData] = useState<MfaSetupResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [verifying, setVerifying] = useState(false);
    const [verificationCode, setVerificationCode] = useState('');
    const [showBackupCodes, setShowBackupCodes] = useState(false);
    const [backupCodes, setBackupCodes] = useState<string[]>([]);
    const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

    useEffect(() => {
        loadStatus();
    }, []);

    const loadStatus = async () => {
        setLoading(true);
        try {
            const res = await mfaApi.getStatus();
            if (res.code === 200 && res.data) {
                setStatus(res.data);
            }
        } catch (error) {
            console.error('加载MFA状态失败', error);
        } finally {
            setLoading(false);
        }
    };

    const handleSetup = async () => {
        setLoading(true);
        try {
            const res = await mfaApi.setup();
            if (res.code === 200 && res.data) {
                setSetupData(res.data);
                setBackupCodes(res.data.backupCodes || []);
                setShowBackupCodes(true);
            } else {
                setMessage({ type: 'error', text: res.message || '设置失败' });
            }
        } catch (error: any) {
            setMessage({ type: 'error', text: error?.response?.data?.message || '设置失败' });
        } finally {
            setLoading(false);
        }
    };

    const handleVerify = async () => {
        if (!verificationCode.trim()) {
            setMessage({ type: 'error', text: '请输入验证码' });
            return;
        }

        setVerifying(true);
        try {
            const res = await mfaApi.verify(verificationCode.trim());
            if (res.code === 200) {
                setMessage({ type: 'success', text: 'MFA已启用' });
                setSetupData(null);
                setVerificationCode('');
                loadStatus();
            } else {
                setMessage({ type: 'error', text: res.message || '验证失败' });
            }
        } catch (error: any) {
            setMessage({ type: 'error', text: error?.response?.data?.message || '验证失败' });
        } finally {
            setVerifying(false);
        }
    };

    const handleDisable = async () => {
        if (!window.confirm('确认禁用MFA吗？禁用后将降低账户安全性。')) {
            return;
        }

        setLoading(true);
        try {
            const res = await mfaApi.disable();
            if (res.code === 200) {
                setMessage({ type: 'success', text: 'MFA已禁用' });
                loadStatus();
            } else {
                setMessage({ type: 'error', text: res.message || '禁用失败' });
            }
        } catch (error: any) {
            setMessage({ type: 'error', text: error?.response?.data?.message || '禁用失败' });
        } finally {
            setLoading(false);
        }
    };

    const handleLoadBackupCodes = async () => {
        try {
            const res = await mfaApi.getBackupCodes();
            if (res.code === 200 && res.data) {
                setBackupCodes(res.data);
                setShowBackupCodes(true);
            }
        } catch (error: any) {
            setMessage({ type: 'error', text: error?.response?.data?.message || '加载备用码失败' });
        }
    };

    const handleCopyBackupCodes = () => {
        const text = backupCodes.join('\n');
        navigator.clipboard.writeText(text).then(() => {
            setMessage({ type: 'success', text: '备用码已复制到剪贴板' });
        });
    };

    const handleDownloadBackupCodes = () => {
        const text = backupCodes.join('\n');
        const blob = new Blob([text], { type: 'text/plain' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'mfa-backup-codes.txt';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
    };

    const handlePrintBackupCodes = () => {
        const printWindow = window.open('', '_blank');
        if (printWindow) {
            printWindow.document.write(`
                <html>
                    <head><title>MFA备用码</title></head>
                    <body style="font-family: monospace; padding: 20px;">
                        <h2>MFA备用码</h2>
                        <p>请妥善保管这些备用码，每个码只能使用一次：</p>
                        <pre>${backupCodes.join('\n')}</pre>
                        <p><small>生成时间: ${new Date().toLocaleString('zh-CN')}</small></p>
                    </body>
                </html>
            `);
            printWindow.document.close();
            printWindow.print();
        }
    };

    if (loading && !status) {
        return (
            <div className="flex items-center justify-center h-64">
                <Loader2 className="animate-spin text-slate-400" size={32} />
            </div>
        );
    }

    const isMfaEnabled = status?.enabled || false;

    return (
        <div className="p-6 space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-slate-800 flex items-center">
                        <Shield className="mr-2" size={28} />
                        多因素认证 (MFA)
                    </h2>
                    <p className="text-slate-500 text-sm mt-1">使用TOTP验证器应用增强账户安全性</p>
                </div>
            </div>

            {/* 消息提示 */}
            {message && (
                <div className={`p-4 rounded-lg flex items-center gap-2 ${
                    message.type === 'success' 
                        ? 'bg-green-50 text-green-800 border border-green-200' 
                        : 'bg-red-50 text-red-800 border border-red-200'
                }`}>
                    {message.type === 'success' ? (
                        <CheckCircle2 size={20} />
                    ) : (
                        <XCircle size={20} />
                    )}
                    <span>{message.text}</span>
                    <button
                        onClick={() => setMessage(null)}
                        className="ml-auto text-slate-400 hover:text-slate-600"
                    >
                        ×
                    </button>
                </div>
            )}

            {/* MFA状态 */}
            <div className="bg-white border border-slate-200 rounded-lg p-6">
                <div className="flex items-center justify-between mb-4">
                    <div>
                        <h3 className="text-lg font-semibold">MFA状态</h3>
                        <p className="text-sm text-slate-500 mt-1">
                            {isMfaEnabled 
                                ? 'MFA已启用，您的账户受到双重保护' 
                                : 'MFA未启用，建议启用以增强账户安全性'}
                        </p>
                    </div>
                    <div className={`px-4 py-2 rounded-lg ${
                        isMfaEnabled 
                            ? 'bg-green-100 text-green-700' 
                            : 'bg-yellow-100 text-yellow-700'
                    }`}>
                        {isMfaEnabled ? '已启用' : '未启用'}
                    </div>
                </div>

                {!isMfaEnabled && !setupData && (
                    <button
                        onClick={handleSetup}
                        disabled={loading}
                        className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {loading ? <Loader2 className="animate-spin inline mr-2" size={16} /> : null}
                        启用MFA
                    </button>
                )}

                {isMfaEnabled && (
                    <div className="space-y-4">
                        <button
                            onClick={handleLoadBackupCodes}
                            className="px-4 py-2 border border-slate-300 rounded-lg text-slate-700 hover:bg-slate-50"
                        >
                            查看备用码
                        </button>
                        <button
                            onClick={handleDisable}
                            disabled={loading}
                            className="ml-3 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {loading ? <Loader2 className="animate-spin inline mr-2" size={16} /> : null}
                            禁用MFA
                        </button>
                    </div>
                )}
            </div>

            {/* 设置流程：二维码和验证码输入 */}
            {setupData && !isMfaEnabled && (
                <>
                    <div className="bg-white border border-slate-200 rounded-lg p-6">
                        <h3 className="text-lg font-semibold mb-4">步骤1: 扫描二维码</h3>
                        <div className="flex flex-col items-center space-y-4">
                            <div className="p-4 bg-white border-2 border-slate-200 rounded-lg">
                                <img 
                                    src={setupData.qrCodeUrl} 
                                    alt="MFA QR Code" 
                                    className="w-64 h-64"
                                />
                            </div>
                            <p className="text-sm text-slate-600 text-center max-w-md">
                                使用验证器应用（如 Google Authenticator、Microsoft Authenticator）扫描上方二维码
                            </p>
                        </div>
                    </div>

                    <div className="bg-white border border-slate-200 rounded-lg p-6">
                        <h3 className="text-lg font-semibold mb-4">步骤2: 输入验证码</h3>
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    6位验证码
                                </label>
                                <input
                                    type="text"
                                    value={verificationCode}
                                    onChange={(e) => setVerificationCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                                    placeholder="000000"
                                    maxLength={6}
                                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 text-center text-2xl tracking-widest font-mono"
                                />
                                <p className="text-xs text-slate-500 mt-1">
                                    输入验证器应用中显示的6位数字
                                </p>
                            </div>
                            <button
                                onClick={handleVerify}
                                disabled={verifying || verificationCode.length !== 6}
                                className="w-full px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                            >
                                {verifying ? (
                                    <Loader2 className="animate-spin" size={16} />
                                ) : (
                                    <CheckCircle2 size={16} />
                                )}
                                完成设置
                            </button>
                        </div>
                    </div>
                </>
            )}

            {/* 备用码 */}
            {showBackupCodes && backupCodes.length > 0 && (
                <div className="bg-white border border-slate-200 rounded-lg p-6">
                    <div className="flex items-center justify-between mb-4">
                        <div>
                            <h3 className="text-lg font-semibold">备用码</h3>
                            <p className="text-sm text-slate-500 mt-1">
                                请妥善保管这些备用码，每个码只能使用一次。建议保存到安全的地方。
                            </p>
                        </div>
                        <button
                            onClick={() => setShowBackupCodes(false)}
                            className="text-slate-400 hover:text-slate-600"
                        >
                            <XCircle size={20} />
                        </button>
                    </div>
                    <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-4 flex items-start gap-3">
                        <AlertCircle className="text-yellow-600 flex-shrink-0 mt-0.5" size={20} />
                        <div className="text-sm text-yellow-800">
                            <div className="font-semibold mb-1">重要提示</div>
                            <div>这些备用码现在只显示一次，请务必保存好。如果丢失，您可能需要重新设置MFA。</div>
                        </div>
                    </div>
                    <div className="grid grid-cols-2 gap-3 mb-4 p-4 bg-slate-50 rounded-lg">
                        {backupCodes.map((code, index) => (
                            <div key={index} className="font-mono text-sm text-center p-2 bg-white rounded border">
                                {code}
                            </div>
                        ))}
                    </div>
                    <div className="flex gap-3">
                        <button
                            onClick={handleCopyBackupCodes}
                            className="px-4 py-2 border border-slate-300 rounded-lg text-slate-700 hover:bg-slate-50 flex items-center gap-2"
                        >
                            <Copy size={16} />
                            复制
                        </button>
                        <button
                            onClick={handleDownloadBackupCodes}
                            className="px-4 py-2 border border-slate-300 rounded-lg text-slate-700 hover:bg-slate-50 flex items-center gap-2"
                        >
                            <Download size={16} />
                            下载
                        </button>
                        <button
                            onClick={handlePrintBackupCodes}
                            className="px-4 py-2 border border-slate-300 rounded-lg text-slate-700 hover:bg-slate-50 flex items-center gap-2"
                        >
                            <Printer size={16} />
                            打印
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default MfaSettingsPage;



