import React, { useState } from 'react';
// Local Card Component since common/Card doesn't exist
const Card: React.FC<{ className?: string; children: React.ReactNode }> = ({ className = '', children }) => (
    <div className={`bg-white shadow rounded-lg p-6 ${className}`}>
        {children}
    </div>
);
import { client } from '../../api/client';
import { toast } from 'react-hot-toast';
import { CloudDownload, Code } from 'lucide-react';

const PaymentFileTestView: React.FC = () => {
    const [configId, setConfigId] = useState('1'); // Default to 1 (YonSuite)
    const [fileIds, setFileIds] = useState('mfdfs23421\nn95sdavdas'); // Default from doc
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState<any>(null);

    const handleTest = async () => {
        setLoading(true);
        setResult(null);
        try {
            const ids = fileIds.split('\n').map(s => s.trim()).filter(Boolean);
            if (ids.length === 0) {
                toast.error('请输入至少一个文件ID');
                return;
            }

            const response = await client.post('/api/integration/yon/payment/url/batch', {
                configId: parseInt(configId),
                fileIds: ids
            });
            setResult(response.data);
            toast.success('请求成功');
        } catch (error: any) {
            console.error(error);
            toast.error('请求失败: ' + (error.response?.data?.message || error.message));
            setResult({ error: error.message, detail: error.response?.data });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="space-y-6 p-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900">YonBIP Payment File Test</h1>
                    <p className="text-slate-500 mt-1">验证 AI 生成的资金结算文件下载接口 (Pass 2 Result Verification)</p>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Input Panel */}
                <Card>
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-1">
                                ERP Configuration ID
                            </label>
                            <input
                                type="number"
                                value={configId}
                                onChange={e => setConfigId(e.target.value)}
                                className="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-indigo-500"
                            />
                            <p className="text-xs text-slate-500 mt-1">关联 sys_erp_config 表的主键 ID</p>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-1">
                                File IDs (每行一个)
                            </label>
                            <textarea
                                value={fileIds}
                                onChange={e => setFileIds(e.target.value)}
                                rows={8}
                                className="w-full px-3 py-2 border rounded-md font-mono text-sm focus:ring-2 focus:ring-indigo-500"
                                placeholder="输入文件ID..."
                            />
                        </div>

                        <button
                            onClick={handleTest}
                            disabled={loading}
                            className="w-full flex items-center justify-center space-x-2 bg-indigo-600 hover:bg-indigo-700 text-white py-2 px-4 rounded-md disabled:opacity-50 transition-colors"
                        >
                            {loading ? (
                                <span className="animate-spin">⌛</span>
                            ) : (
                                <CloudDownload className="w-5 h-5" />
                            )}
                            <span>发送请求 (Invoke Generated Service)</span>
                        </button>
                    </div>
                </Card>

                {/* Result Panel */}
                <Card className="bg-slate-50">
                    <div className="flex items-center space-x-2 mb-4 text-slate-700">
                        <Code className="w-5 h-5" />
                        <h3 className="font-semibold">Test Result</h3>
                    </div>

                    {result ? (
                        <pre className="bg-slate-900 text-green-400 p-4 rounded-md overflow-auto text-xs font-mono h-[300px]">
                            {JSON.stringify(result, null, 2)}
                        </pre>
                    ) : (
                        <div className="h-[300px] flex items-center justify-center text-slate-400 border-2 border-dashed rounded-md">
                            等待测试结果...
                        </div>
                    )}
                </Card>
            </div>
        </div>
    );
};

export default PaymentFileTestView;
