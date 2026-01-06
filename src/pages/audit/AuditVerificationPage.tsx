// Input: React、lucide-react、auditVerificationApi、useFondsStore
// Output: AuditVerificationPage 组件
// Pos: 审计证据链验真页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState } from 'react';
import { Shield, CheckCircle2, XCircle, Loader2, FileText, Calendar, Search, Shuffle, List } from 'lucide-react';
import { auditVerificationApi, VerificationResult, ChainVerificationResult, SamplingResult } from '../../api/auditVerification';
import { useFondsStore } from '../../store';

type VerifyMode = 'single' | 'batch' | 'chain' | 'sample';

/**
 * 审计证据链验真页面
 * 
 * 功能：
 * 1. 单条日志验真
 * 2. 批量日志验真
 * 3. 按条件验真（全宗、时间范围、操作类型）
 * 4. 随机抽检验真
 * 5. 验真结果展示
 * 
 * PRD 来源: Section 6.2 - 审计日志防篡改要求
 */
export const AuditVerificationPage: React.FC = () => {
    const { currentFonds } = useFondsStore();
    const [mode, setMode] = useState<VerifyMode>('single');
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState<VerificationResult | ChainVerificationResult | SamplingResult | null>(null);
    const [error, setError] = useState<string | null>(null);

    // 单条验真
    const [singleLogId, setSingleLogId] = useState('');

    // 批量验真
    const [batchLogIds, setBatchLogIds] = useState('');

    // 链路验真
    const [chainStartDate, setChainStartDate] = useState('');
    const [chainEndDate, setChainEndDate] = useState('');
    const [chainFondsNo, setChainFondsNo] = useState('');

    // 抽检验真
    const [sampleSize, setSampleSize] = useState(10);
    const [sampleStartDate, setSampleStartDate] = useState('');
    const [sampleEndDate, setSampleEndDate] = useState('');

    const handleSingleVerify = async () => {
        if (!singleLogId.trim()) {
            setError('请输入日志ID');
            return;
        }
        setLoading(true);
        setError(null);
        try {
            const res = await auditVerificationApi.verifySingle(singleLogId.trim());
            if (res.code === 200 && res.data) {
                setResult(res.data);
            } else {
                setError(res.message || '验真失败');
            }
        } catch (err: any) {
            setError(err.message || '验真失败');
        } finally {
            setLoading(false);
        }
    };

    const handleBatchVerify = async () => {
        const ids = batchLogIds.split('\n').map(id => id.trim()).filter(id => id);
        if (ids.length === 0) {
            setError('请输入至少一个日志ID');
            return;
        }
        setLoading(true);
        setError(null);
        try {
            const res = await auditVerificationApi.verifyChainByIds(ids);
            if (res.code === 200 && res.data) {
                setResult(res.data);
            } else {
                setError(res.message || '验真失败');
            }
        } catch (err: any) {
            setError(err.message || '验真失败');
        } finally {
            setLoading(false);
        }
    };

    const handleChainVerify = async () => {
        if (!chainStartDate || !chainEndDate) {
            setError('请选择开始日期和结束日期');
            return;
        }
        setLoading(true);
        setError(null);
        try {
            const res = await auditVerificationApi.verifyChain(
                chainStartDate,
                chainEndDate,
                chainFondsNo || undefined
            );
            if (res.code === 200 && res.data) {
                setResult(res.data);
            } else {
                setError(res.message || '验真失败');
            }
        } catch (err: any) {
            setError(err.message || '验真失败');
        } finally {
            setLoading(false);
        }
    };

    const handleSampleVerify = async () => {
        if (!sampleStartDate || !sampleEndDate) {
            setError('请选择开始日期和结束日期');
            return;
        }
        if (sampleSize <= 0) {
            setError('抽检数量必须大于0');
            return;
        }
        setLoading(true);
        setError(null);
        try {
            const res = await auditVerificationApi.sampleVerify(
                sampleSize,
                sampleStartDate,
                sampleEndDate
            );
            if (res.code === 200 && res.data) {
                setResult(res.data);
            } else {
                setError(res.message || '验真失败');
            }
        } catch (err: any) {
            setError(err.message || '验真失败');
        } finally {
            setLoading(false);
        }
    };

    const renderResult = () => {
        if (!result) return null;

        // 单条验真结果
        if ('logId' in result && !('totalLogs' in result)) {
            const singleResult = result as VerificationResult;
            return (
                <div className="mt-6 bg-white border border-slate-200 rounded-lg p-6">
                    <h3 className="text-lg font-semibold mb-4 flex items-center">
                        <FileText className="mr-2" size={20} />
                        验真结果
                    </h3>
                    <div className={`p-4 rounded-lg ${singleResult.valid ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'}`}>
                        <div className="flex items-center mb-2">
                            {singleResult.valid ? (
                                <CheckCircle2 className="text-green-600 mr-2" size={20} />
                            ) : (
                                <XCircle className="text-red-600 mr-2" size={20} />
                            )}
                            <span className={`font-semibold ${singleResult.valid ? 'text-green-800' : 'text-red-800'}`}>
                                {singleResult.valid ? '验证通过' : '验证失败'}
                            </span>
                        </div>
                        <div className="space-y-2 text-sm">
                            <div><span className="text-slate-600">日志ID:</span> <span className="font-mono">{singleResult.logId}</span></div>
                            {singleResult.expectedHash && (
                                <div><span className="text-slate-600">期望哈希:</span> <span className="font-mono text-xs">{singleResult.expectedHash}</span></div>
                            )}
                            {singleResult.actualHash && (
                                <div><span className="text-slate-600">实际哈希:</span> <span className="font-mono text-xs">{singleResult.actualHash}</span></div>
                            )}
                            {singleResult.reason && (
                                <div><span className="text-slate-600">失败原因:</span> <span className="text-red-600">{singleResult.reason}</span></div>
                            )}
                            {singleResult.verifiedAt && (
                                <div><span className="text-slate-600">验证时间:</span> {singleResult.verifiedAt}</div>
                            )}
                        </div>
                    </div>
                </div>
            );
        }

        // 哈希链/抽检验真结果
        if ('totalLogs' in result) {
            const chainResult = 'verificationResult' in result 
                ? (result as SamplingResult).verificationResult
                : result as ChainVerificationResult;
            
            const sampleInfo = 'verificationResult' in result ? result as SamplingResult : null;

            return (
                <div className="mt-6 bg-white border border-slate-200 rounded-lg p-6">
                    <h3 className="text-lg font-semibold mb-4 flex items-center">
                        <FileText className="mr-2" size={20} />
                        验真结果
                    </h3>
                    {sampleInfo && (
                        <div className="mb-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                            <div className="text-sm space-y-1">
                                <div><span className="text-slate-600">总日志数:</span> {sampleInfo.totalLogs}</div>
                                <div><span className="text-slate-600">抽检数量:</span> {sampleInfo.sampledLogs}</div>
                                <div><span className="text-slate-600">抽检日志ID:</span> 
                                    <div className="mt-1 font-mono text-xs max-h-32 overflow-y-auto">
                                        {sampleInfo.sampledLogIds.join(', ')}
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}
                    <div className={`p-4 rounded-lg ${chainResult.chainIntact ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'}`}>
                        <div className="flex items-center mb-4">
                            {chainResult.chainIntact ? (
                                <CheckCircle2 className="text-green-600 mr-2" size={20} />
                            ) : (
                                <XCircle className="text-red-600 mr-2" size={20} />
                            )}
                            <span className={`font-semibold ${chainResult.chainIntact ? 'text-green-800' : 'text-red-800'}`}>
                                {chainResult.chainIntact ? '哈希链完整' : '哈希链断裂'}
                            </span>
                        </div>
                        <div className="grid grid-cols-3 gap-4 mb-4">
                            <div className="text-center p-3 bg-white rounded">
                                <div className="text-2xl font-bold text-slate-800">{chainResult.totalLogs}</div>
                                <div className="text-xs text-slate-600 mt-1">总日志数</div>
                            </div>
                            <div className="text-center p-3 bg-white rounded">
                                <div className="text-2xl font-bold text-green-600">{chainResult.validLogs}</div>
                                <div className="text-xs text-slate-600 mt-1">有效日志</div>
                            </div>
                            <div className="text-center p-3 bg-white rounded">
                                <div className="text-2xl font-bold text-red-600">{chainResult.invalidLogs}</div>
                                <div className="text-xs text-slate-600 mt-1">无效日志</div>
                            </div>
                        </div>
                        {chainResult.invalidResults && chainResult.invalidResults.length > 0 && (
                            <div className="mt-4">
                                <div className="text-sm font-semibold text-red-800 mb-2">无效日志详情:</div>
                                <div className="space-y-2 max-h-64 overflow-y-auto">
                                    {chainResult.invalidResults.map((invalid, idx) => (
                                        <div key={idx} className="p-3 bg-white border border-red-200 rounded text-sm">
                                            <div className="font-mono text-xs mb-1">ID: {invalid.logId}</div>
                                            <div className="text-red-600">{invalid.reason}</div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}
                        {chainResult.verifiedAt && (
                            <div className="mt-4 text-sm text-slate-600">
                                验证时间: {chainResult.verifiedAt}
                            </div>
                        )}
                    </div>
                </div>
            );
        }

        return null;
    };

    return (
        <div className="p-6 space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-slate-800 flex items-center">
                        <Shield className="mr-2" size={28} />
                        审计证据链验真
                    </h2>
                    <p className="text-slate-500 text-sm mt-1">验证审计日志的完整性和真实性，确保未被篡改</p>
                </div>
            </div>

            {/* 模式选择 */}
            <div className="bg-white border border-slate-200 rounded-lg p-4">
                <div className="flex space-x-2">
                    <button
                        onClick={() => { setMode('single'); setResult(null); setError(null); }}
                        className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                            mode === 'single' 
                                ? 'bg-primary-600 text-white' 
                                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                        }`}
                    >
                        <Search className="inline mr-2" size={16} />
                        单条验真
                    </button>
                    <button
                        onClick={() => { setMode('batch'); setResult(null); setError(null); }}
                        className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                            mode === 'batch' 
                                ? 'bg-primary-600 text-white' 
                                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                        }`}
                    >
                        <List className="inline mr-2" size={16} />
                        批量验真
                    </button>
                    <button
                        onClick={() => { setMode('chain'); setResult(null); setError(null); }}
                        className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                            mode === 'chain' 
                                ? 'bg-primary-600 text-white' 
                                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                        }`}
                    >
                        <FileText className="inline mr-2" size={16} />
                        链路验真
                    </button>
                    <button
                        onClick={() => { setMode('sample'); setResult(null); setError(null); }}
                        className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                            mode === 'sample' 
                                ? 'bg-primary-600 text-white' 
                                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                        }`}
                    >
                        <Shuffle className="inline mr-2" size={16} />
                        抽检验真
                    </button>
                </div>
            </div>

            {/* 单条验真 */}
            {mode === 'single' && (
                <div className="bg-white border border-slate-200 rounded-lg p-6">
                    <h3 className="text-lg font-semibold mb-4">单条日志验真</h3>
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-2">
                                日志ID
                            </label>
                            <input
                                type="text"
                                value={singleLogId}
                                onChange={(e) => setSingleLogId(e.target.value)}
                                placeholder="请输入审计日志ID"
                                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                            />
                        </div>
                        <button
                            onClick={handleSingleVerify}
                            disabled={loading}
                            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
                        >
                            {loading ? <Loader2 className="animate-spin mr-2" size={16} /> : <Search className="mr-2" size={16} />}
                            开始验真
                        </button>
                    </div>
                </div>
            )}

            {/* 批量验真 */}
            {mode === 'batch' && (
                <div className="bg-white border border-slate-200 rounded-lg p-6">
                    <h3 className="text-lg font-semibold mb-4">批量日志验真</h3>
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-2">
                                日志ID列表（每行一个）
                            </label>
                            <textarea
                                value={batchLogIds}
                                onChange={(e) => setBatchLogIds(e.target.value)}
                                placeholder="请输入日志ID，每行一个"
                                rows={8}
                                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 font-mono text-sm"
                            />
                        </div>
                        <button
                            onClick={handleBatchVerify}
                            disabled={loading}
                            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
                        >
                            {loading ? <Loader2 className="animate-spin mr-2" size={16} /> : <List className="mr-2" size={16} />}
                            开始验真
                        </button>
                    </div>
                </div>
            )}

            {/* 链路验真 */}
            {mode === 'chain' && (
                <div className="bg-white border border-slate-200 rounded-lg p-6">
                    <h3 className="text-lg font-semibold mb-4 flex items-center">
                        <Calendar className="mr-2" size={20} />
                        按时间范围验真
                    </h3>
                    <div className="space-y-4">
                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    开始日期 <span className="text-red-500">*</span>
                                </label>
                                <input
                                    type="date"
                                    value={chainStartDate}
                                    onChange={(e) => setChainStartDate(e.target.value)}
                                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    结束日期 <span className="text-red-500">*</span>
                                </label>
                                <input
                                    type="date"
                                    value={chainEndDate}
                                    onChange={(e) => setChainEndDate(e.target.value)}
                                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                                />
                            </div>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-2">
                                全宗号（可选）
                            </label>
                            <input
                                type="text"
                                value={chainFondsNo}
                                onChange={(e) => setChainFondsNo(e.target.value)}
                                placeholder="留空则验证所有全宗"
                                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                            />
                        </div>
                        <button
                            onClick={handleChainVerify}
                            disabled={loading}
                            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
                        >
                            {loading ? <Loader2 className="animate-spin mr-2" size={16} /> : <FileText className="mr-2" size={16} />}
                            开始验真
                        </button>
                    </div>
                </div>
            )}

            {/* 抽检验真 */}
            {mode === 'sample' && (
                <div className="bg-white border border-slate-200 rounded-lg p-6">
                    <h3 className="text-lg font-semibold mb-4 flex items-center">
                        <Shuffle className="mr-2" size={20} />
                        随机抽检验真
                    </h3>
                    <div className="space-y-4">
                        <div className="grid grid-cols-3 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    开始日期 <span className="text-red-500">*</span>
                                </label>
                                <input
                                    type="date"
                                    value={sampleStartDate}
                                    onChange={(e) => setSampleStartDate(e.target.value)}
                                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    结束日期 <span className="text-red-500">*</span>
                                </label>
                                <input
                                    type="date"
                                    value={sampleEndDate}
                                    onChange={(e) => setSampleEndDate(e.target.value)}
                                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">
                                    抽检数量 <span className="text-red-500">*</span>
                                </label>
                                <input
                                    type="number"
                                    value={sampleSize}
                                    onChange={(e) => setSampleSize(parseInt(e.target.value) || 0)}
                                    min={1}
                                    max={1000}
                                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                                />
                            </div>
                        </div>
                        <button
                            onClick={handleSampleVerify}
                            disabled={loading}
                            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
                        >
                            {loading ? <Loader2 className="animate-spin mr-2" size={16} /> : <Shuffle className="mr-2" size={16} />}
                            开始抽检验真
                        </button>
                    </div>
                </div>
            )}

            {/* 错误提示 */}
            {error && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                    <div className="flex items-center text-red-800">
                        <XCircle className="mr-2" size={20} />
                        <span>{error}</span>
                    </div>
                </div>
            )}

            {/* 结果显示 */}
            {renderResult()}
        </div>
    );
};

export default AuditVerificationPage;





