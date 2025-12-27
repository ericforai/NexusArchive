// Input: React
// Output: React 组件 VolumeManagement
// Pos: src/pages/operations/VolumeManagement.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import {
    FolderPlus,
    FileCheck,
    Send,
    CheckCircle,
    XCircle,
    FileText,
    ChevronRight,
    Calendar,
    Archive,
    RefreshCw,
    Download
} from 'lucide-react';

interface Volume {
    id: string;
    volumeCode: string;
    title: string;
    fondsNo: string;
    fiscalYear: string;
    fiscalPeriod: string;
    categoryCode: string;
    fileCount: number;
    retentionPeriod: string;
    status: string;
    reviewedBy?: string;
    reviewedAt?: string;
    archivedAt?: string;
    createdTime: string;
}

interface VolumeFile {
    id: string;
    archiveCode: string;
    title: string;
    creator: string;
    amount: number;
    docDate: string;
    status: string;
}

import { client } from '../../api/client';

// ... (imports)

// Remove API_BASE constant

export default function VolumeManagement() {
    const [volumes, setVolumes] = useState<Volume[]>([]);
    const [selectedVolume, setSelectedVolume] = useState<Volume | null>(null);
    const [volumeFiles, setVolumeFiles] = useState<VolumeFile[]>([]);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
    const [assembleModal, setAssembleModal] = useState(false);
    const [fiscalPeriod, setFiscalPeriod] = useState('');
    const [registrationForm, setRegistrationForm] = useState<any>(null);
    const [showFormModal, setShowFormModal] = useState(false);

    useEffect(() => {
        loadVolumes();
    }, []);

    const loadVolumes = async () => {
        setLoading(true);
        console.log('[VolumeManagement] Loading volumes...');
        try {
            const res = await client.get('/volumes');
            console.log('[VolumeManagement] API Response:', res);
            if (res.data.code === 200) {
                console.log('[VolumeManagement] Setting volumes:', res.data.data.records);
                setVolumes(res.data.data.records || []);
            } else {
                console.warn('[VolumeManagement] API returned non-200 code:', res.data);
            }
        } catch (error) {
            console.error('[VolumeManagement] API Error:', error);
            showMessage('error', '加载案卷列表失败');
        }
        setLoading(false);
    };

    const loadVolumeFiles = async (volumeId: string) => {
        try {
            const res = await client.get(`/volumes/${volumeId}/files`);
            if (res.data.code === 200) {
                setVolumeFiles(res.data.data || []);
            }
        } catch (error) {
            showMessage('error', '加载卷内文件失败');
        }
    };

    const showMessage = (type: 'success' | 'error', text: string) => {
        setMessage({ type, text });
        setTimeout(() => setMessage(null), 3000);
    };

    const handleAssemble = async () => {
        if (!fiscalPeriod) {
            showMessage('error', '请输入会计期间');
            return;
        }
        setLoading(true);
        try {
            const res = await client.post('/volumes/assemble', { fiscalPeriod });
            if (res.data.code === 200) {
                showMessage('success', `组卷成功: ${res.data.data.volumeCode}`);
                setAssembleModal(false);
                setFiscalPeriod('');
                loadVolumes();
            } else {
                showMessage('error', res.data.message || '组卷失败');
            }
        } catch (error) {
            showMessage('error', '组卷请求失败');
        }
        setLoading(false);
    };

    const handleSubmitReview = async (volumeId: string) => {
        setLoading(true);
        try {
            const res = await client.post(`/volumes/${volumeId}/submit-review`);
            if (res.data.code === 200) {
                showMessage('success', '已提交审核');
                loadVolumes();
            } else {
                showMessage('error', res.data.message || '提交失败');
            }
        } catch (error) {
            showMessage('error', '提交审核失败');
        }
        setLoading(false);
    };

    const handleApprove = async (volumeId: string) => {
        setLoading(true);
        try {
            const res = await client.post(`/volumes/${volumeId}/approve?reviewerId=admin`);
            if (res.data.code === 200) {
                showMessage('success', '归档成功');
                loadVolumes();
                if (selectedVolume?.id === volumeId) {
                    loadVolumeFiles(volumeId);
                }
            } else {
                showMessage('error', res.data.message || '归档失败');
            }
        } catch (error) {
            showMessage('error', '归档请求失败');
        }
        setLoading(false);
    };

    const handleReject = async (volumeId: string) => {
        const reason = prompt('请输入驳回原因:');
        if (!reason) return;

        setLoading(true);
        try {
            const res = await client.post(`/volumes/${volumeId}/reject`, { reviewerId: 'admin', reason });
            if (res.data.code === 200) {
                showMessage('success', '已驳回');
                loadVolumes();
            } else {
                showMessage('error', res.data.message || '驳回失败');
            }
        } catch (error) {
            showMessage('error', '驳回请求失败');
        }
        setLoading(false);
    };

    const handleViewRegistration = async (volumeId: string) => {
        try {
            const res = await client.get(`/volumes/${volumeId}/registration-form`);
            if (res.data.code === 200) {
                setRegistrationForm(res.data.data);
                setShowFormModal(true);
            }
        } catch (error) {
            showMessage('error', '获取归档登记表失败');
        }
    };

    const handleExportAip = async (volumeId: string) => {
        try {
            showMessage('success', '正在生成 AIP 包，请稍候...');
            const res = await client.get(`/volumes/${volumeId}/export-aip`, {
                responseType: 'blob'
            });

            // Axios response is already checked by interceptor for 401, but we check status here
            if (res.status === 200) {
                const blob = res.data;
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                // 从响应头获取文件名，或者使用默认名
                const contentDisposition = res.headers['content-disposition'];
                let filename = `${volumeId}_AIP.zip`;
                if (contentDisposition) {
                    const match = contentDisposition.match(/filename="?([^"]+)"?/);
                    if (match && match[1]) {
                        filename = match[1];
                    }
                }
                a.download = filename;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
                showMessage('success', 'AIP 包下载成功');
            } else {
                showMessage('error', '导出 AIP 包失败');
            }
        } catch (error) {
            showMessage('error', '导出请求失败');
        }
    };

    const selectVolume = (volume: Volume) => {
        setSelectedVolume(volume);
        loadVolumeFiles(volume.id);
    };

    const getStatusBadge = (status: string) => {
        const styles: Record<string, string> = {
            draft: 'bg-gray-500',
            pending: 'bg-yellow-500',
            archived: 'bg-green-500'
        };
        const labels: Record<string, string> = {
            draft: '草稿',
            pending: '待审核',
            archived: '已归档'
        };
        return (
            <span className={`px-2 py-1 rounded text-xs text-white ${styles[status] || 'bg-gray-400'}`}>
                {labels[status] || status}
            </span>
        );
    };

    return (
        <div className="p-6 bg-gray-900 min-h-screen text-white">
            {/* 消息提示 */}
            {message && (
                <div className={`fixed top-4 right-4 p-4 rounded-lg shadow-lg z-50 ${message.type === 'success' ? 'bg-green-600' : 'bg-red-600'
                    }`}>
                    {message.text}
                </div>
            )}

            {/* 头部 */}
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-2xl font-bold flex items-center gap-2">
                    <Archive className="w-6 h-6" />
                    档案组卷管理
                </h1>
                <div className="flex gap-2">
                    <button
                        onClick={loadVolumes}
                        className="flex items-center gap-2 px-4 py-2 bg-gray-700 rounded-lg hover:bg-gray-600"
                    >
                        <RefreshCw className="w-4 h-4" />
                        刷新
                    </button>
                    <button
                        onClick={() => setAssembleModal(true)}
                        className="flex items-center gap-2 px-4 py-2 bg-blue-600 rounded-lg hover:bg-blue-500"
                    >
                        <FolderPlus className="w-4 h-4" />
                        按月组卷
                    </button>
                </div>
            </div>

            <div className="grid grid-cols-12 gap-6">
                {/* 案卷列表 */}
                <div className="col-span-5 bg-gray-800 rounded-lg p-4">
                    <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
                        <FolderPlus className="w-5 h-5" />
                        案卷列表
                    </h2>

                    {loading && <div className="text-center py-4">加载中...</div>}

                    {volumes.length === 0 && !loading && (
                        <div className="text-center py-8 text-gray-400">
                            暂无案卷，点击"按月组卷"创建
                        </div>
                    )}

                    <div className="space-y-2">
                        {volumes.map(volume => (
                            <div
                                key={volume.id}
                                onClick={() => selectVolume(volume)}
                                className={`p-4 rounded-lg cursor-pointer transition ${selectedVolume?.id === volume.id
                                    ? 'bg-blue-600/30 border border-blue-500'
                                    : 'bg-gray-700 hover:bg-gray-600'
                                    }`}
                            >
                                <div className="flex justify-between items-start mb-2">
                                    <span className="font-medium">{volume.volumeCode}</span>
                                    {getStatusBadge(volume.status)}
                                </div>
                                <div className="text-sm text-gray-300">{volume.title}</div>
                                <div className="flex justify-between text-xs text-gray-400 mt-2">
                                    <span>凭证数: {volume.fileCount}</span>
                                    <span>保管期限: {volume.retentionPeriod}</span>
                                </div>

                                {/* 操作按钮 */}
                                <div className="flex gap-2 mt-3 pt-2 border-t border-gray-600">
                                    {volume.status === 'draft' && (
                                        <button
                                            onClick={(e) => { e.stopPropagation(); handleSubmitReview(volume.id); }}
                                            className="flex-1 flex items-center justify-center gap-1 px-2 py-1 bg-yellow-600 rounded text-xs hover:bg-yellow-500"
                                        >
                                            <Send className="w-3 h-3" /> 提交审核
                                        </button>
                                    )}
                                    {volume.status === 'pending' && (
                                        <>
                                            <button
                                                onClick={(e) => { e.stopPropagation(); handleApprove(volume.id); }}
                                                className="flex-1 flex items-center justify-center gap-1 px-2 py-1 bg-green-600 rounded text-xs hover:bg-green-500"
                                            >
                                                <CheckCircle className="w-3 h-3" /> 归档
                                            </button>
                                            <button
                                                onClick={(e) => { e.stopPropagation(); handleReject(volume.id); }}
                                                className="flex-1 flex items-center justify-center gap-1 px-2 py-1 bg-red-600 rounded text-xs hover:bg-red-500"
                                            >
                                                <XCircle className="w-3 h-3" /> 驳回
                                            </button>
                                        </>
                                    )}
                                    {volume.status === 'archived' && (
                                        <>
                                            <button
                                                onClick={(e) => { e.stopPropagation(); handleViewRegistration(volume.id); }}
                                                className="flex-1 flex items-center justify-center gap-1 px-2 py-1 bg-purple-600 rounded text-xs hover:bg-purple-500"
                                            >
                                                <FileText className="w-3 h-3" /> 归档登记表
                                            </button>
                                            <button
                                                onClick={(e) => { e.stopPropagation(); handleExportAip(volume.id); }}
                                                className="flex-1 flex items-center justify-center gap-1 px-2 py-1 bg-blue-600 rounded text-xs hover:bg-blue-500"
                                            >
                                                <Download className="w-3 h-3" /> 导出 AIP
                                            </button>
                                        </>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* 卷内文件 */}
                <div className="col-span-7 bg-gray-800 rounded-lg p-4">
                    <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
                        <FileCheck className="w-5 h-5" />
                        卷内文件清单
                        {selectedVolume && (
                            <span className="text-sm font-normal text-gray-400">
                                ({selectedVolume.volumeCode})
                            </span>
                        )}
                    </h2>

                    {!selectedVolume && (
                        <div className="text-center py-8 text-gray-400">
                            请从左侧选择一个案卷查看详情
                        </div>
                    )}

                    {selectedVolume && volumeFiles.length === 0 && (
                        <div className="text-center py-8 text-gray-400">
                            暂无卷内文件
                        </div>
                    )}

                    {volumeFiles.length > 0 && (
                        <table className="w-full">
                            <thead>
                                <tr className="text-left text-gray-400 text-sm border-b border-gray-700">
                                    <th className="pb-2">序号</th>
                                    <th className="pb-2">档号</th>
                                    <th className="pb-2">题名</th>
                                    <th className="pb-2">制单人</th>
                                    <th className="pb-2">金额</th>
                                    <th className="pb-2">日期</th>
                                    <th className="pb-2">状态</th>
                                </tr>
                            </thead>
                            <tbody>
                                {volumeFiles.map((file, index) => (
                                    <tr key={file.id} className="border-b border-gray-700/50 hover:bg-gray-700/30">
                                        <td className="py-3 text-gray-400">{index + 1}</td>
                                        <td className="py-3">{file.archiveCode}</td>
                                        <td className="py-3">{file.title}</td>
                                        <td className="py-3 text-gray-300">{file.creator}</td>
                                        <td className="py-3 text-yellow-400">¥{file.amount?.toFixed(2)}</td>
                                        <td className="py-3 text-gray-400">{file.docDate}</td>
                                        <td className="py-3">{getStatusBadge(file.status)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>

            {/* 组卷弹窗 */}
            {assembleModal && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                    <div className="bg-gray-800 rounded-lg p-6 w-96">
                        <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
                            <Calendar className="w-5 h-5" />
                            按月组卷
                        </h3>
                        <p className="text-sm text-gray-400 mb-4">
                            将指定期间内未组卷的会计凭证自动组成案卷
                        </p>
                        <input
                            type="month"
                            value={fiscalPeriod}
                            onChange={(e) => setFiscalPeriod(e.target.value)}
                            className="w-full p-3 bg-gray-700 rounded-lg mb-4 text-white"
                            placeholder="选择会计期间"
                        />
                        <div className="flex gap-2">
                            <button
                                onClick={() => setAssembleModal(false)}
                                className="flex-1 px-4 py-2 bg-gray-600 rounded-lg hover:bg-gray-500"
                            >
                                取消
                            </button>
                            <button
                                onClick={handleAssemble}
                                disabled={loading}
                                className="flex-1 px-4 py-2 bg-blue-600 rounded-lg hover:bg-blue-500 disabled:opacity-50"
                            >
                                {loading ? '处理中...' : '开始组卷'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* 归档登记表弹窗 */}
            {showFormModal && registrationForm && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 overflow-y-auto py-8">
                    <div className="bg-gray-800 rounded-lg p-6 w-[800px] max-h-[90vh] overflow-y-auto">
                        <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
                            <FileText className="w-5 h-5" />
                            电子文件归档登记表
                        </h3>

                        <div className="grid grid-cols-2 gap-4 text-sm mb-6">
                            <div className="bg-gray-700 p-3 rounded">
                                <span className="text-gray-400">登记号：</span>
                                {registrationForm.registrationNo}
                            </div>
                            <div className="bg-gray-700 p-3 rounded">
                                <span className="text-gray-400">案卷号：</span>
                                {registrationForm.volumeCode}
                            </div>
                            <div className="bg-gray-700 p-3 rounded col-span-2">
                                <span className="text-gray-400">案卷标题：</span>
                                {registrationForm.volumeTitle}
                            </div>
                            <div className="bg-gray-700 p-3 rounded">
                                <span className="text-gray-400">全宗号：</span>
                                {registrationForm.fondsNo}
                            </div>
                            <div className="bg-gray-700 p-3 rounded">
                                <span className="text-gray-400">会计期间：</span>
                                {registrationForm.fiscalPeriod}
                            </div>
                            <div className="bg-gray-700 p-3 rounded">
                                <span className="text-gray-400">分类：</span>
                                {registrationForm.categoryName}
                            </div>
                            <div className="bg-gray-700 p-3 rounded">
                                <span className="text-gray-400">保管期限：</span>
                                {registrationForm.retentionPeriod}
                            </div>
                            <div className="bg-gray-700 p-3 rounded">
                                <span className="text-gray-400">文件数量：</span>
                                {registrationForm.fileCount} 件
                            </div>
                            <div className="bg-gray-700 p-3 rounded">
                                <span className="text-gray-400">状态：</span>
                                {registrationForm.status === 'archived' ? '已归档' : registrationForm.status}
                            </div>
                        </div>

                        <h4 className="font-semibold mb-2">卷内文件清单</h4>
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="text-left text-gray-400 border-b border-gray-700">
                                    <th className="pb-2">序号</th>
                                    <th className="pb-2">档号</th>
                                    <th className="pb-2">题名</th>
                                    <th className="pb-2">日期</th>
                                    <th className="pb-2">金额</th>
                                    <th className="pb-2">制单人</th>
                                </tr>
                            </thead>
                            <tbody>
                                {registrationForm.fileList?.map((file: any) => (
                                    <tr key={file['序号']} className="border-b border-gray-700/50">
                                        <td className="py-2">{file['序号']}</td>
                                        <td className="py-2">{file['档号']}</td>
                                        <td className="py-2">{file['题名']}</td>
                                        <td className="py-2">{file['日期']}</td>
                                        <td className="py-2">¥{file['金额']?.toFixed(2)}</td>
                                        <td className="py-2">{file['制单人']}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>

                        <div className="flex justify-end mt-4">
                            <button
                                onClick={() => setShowFormModal(false)}
                                className="px-4 py-2 bg-gray-600 rounded-lg hover:bg-gray-500"
                            >
                                关闭
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
