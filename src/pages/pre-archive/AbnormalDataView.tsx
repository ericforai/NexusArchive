// Input: React、本地模块 api/abnormal、AbnormalEditModal
// Output: React 组件 AbnormalDataView
// Pos: src/pages/pre-archive/AbnormalDataView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useState } from 'react';
import { getPendingAbnormals, retryAbnormal, AbnormalVoucher } from '../../api/abnormal';
import AbnormalEditModal from './components/AbnormalEditModal';



const formatDate = (dateStr: string | undefined) => {
    if (!dateStr) return '-';
    try {
        const date = new Date(dateStr.replace(' ', 'T')); // Fix for Safari/Mac
        if (isNaN(date.getTime())) return dateStr;
        return date.toLocaleString('zh-CN');
    } catch {
        return dateStr;
    }
};

const AbnormalDataView: React.FC = () => {
    const [data, setData] = useState<AbnormalVoucher[]>([]);
    const [loading, setLoading] = useState(false);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedItem, setSelectedItem] = useState<AbnormalVoucher | null>(null);

    const fetchData = async () => {
        setLoading(true);
        try {
            const res = await getPendingAbnormals();
            if (res.code === 200) {
                setData(res.data);
            }
        } catch (error) {
            console.error('Failed to fetch abnormal data', error);
        } finally {
            setLoading(false);
        }
    };

    // ... handles ...

    // (This replace is large, let's try a smaller one)


    useEffect(() => {
        fetchData();
    }, []);

    const handleRetry = async (id: string) => {
        try {
            await retryAbnormal(id);
            alert('重试请求已提交');
            fetchData(); // Refresh list
        } catch {
            alert('重试失败');
        }
    };

    const handleEdit = (item: AbnormalVoucher) => {
        setSelectedItem(item);
        setIsModalOpen(true);
    };

    const handleEditSuccess = () => {
        fetchData();
        alert('修改成功，请点击重试以重新处理');
    };

    return (
        <div className="p-6 bg-gray-900 min-h-screen text-white">
            <h1 className="text-2xl font-bold mb-6 text-cyan-400">异常数据隔离区</h1>

            <div className="overflow-x-auto bg-gray-800 rounded-lg shadow-xl border border-gray-700">
                <table className="min-w-full table-auto">
                    <thead className="bg-gray-900/50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">请求ID</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">来源系统</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">凭证号</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">异常原因</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">接收时间</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">状态</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">操作</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-700">
                        {loading ? (
                            <tr><td colSpan={7} className="text-center py-4">加载中...</td></tr>
                        ) : data.length === 0 ? (
                            <tr><td colSpan={7} className="text-center py-4 text-gray-500">暂无异常数据</td></tr>
                        ) : (
                            data.map((item) => (
                                <tr key={item.id} className="hover:bg-gray-700/50 transition-colors">
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-gray-300">{item.requestId}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-300">{item.sourceSystem}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-cyan-300 font-medium">{item.voucherNumber}</td>
                                    <td className="px-6 py-4 text-sm text-red-400 max-w-xs truncate" title={item.failReason}>{item.failReason}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-400">
                                        {formatDate(item.createTime)}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full 
                      ${item.status === 'PENDING' ? 'bg-yellow-900/50 text-yellow-200 border border-yellow-700' :
                                                item.status === 'RETRYING' ? 'bg-blue-900/50 text-blue-200 border border-blue-700' :
                                                    item.status === 'RESOLVED' ? 'bg-green-900/50 text-green-200 border border-green-700' :
                                                        'bg-gray-700 text-gray-300'}`}>
                                            {item.status === 'PENDING' && '待处理'}
                                            {item.status === 'RETRYING' && '重试中'}
                                            {item.status === 'IGNORED' && '已忽略'}
                                            {item.status === 'RESOLVED' && '已解决'}
                                            {!['PENDING', 'RETRYING', 'IGNORED', 'RESOLVED'].includes(item.status) && item.status}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                                        <button
                                            onClick={() => handleRetry(item.id)}
                                            className="text-cyan-400 hover:text-cyan-300 mr-4 transition-colors"
                                        >
                                            重试
                                        </button>
                                        <button
                                            onClick={() => handleEdit(item)}
                                            className="text-indigo-400 hover:text-indigo-300 transition-colors"
                                        >
                                            编辑
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            {selectedItem && (
                <AbnormalEditModal
                    isOpen={isModalOpen}
                    onClose={() => setIsModalOpen(false)}
                    onSuccess={handleEditSuccess}
                    abnormalId={selectedItem.id}
                    initialSipData={selectedItem.sipData}
                />
            )}
        </div>
    );
};

export default AbnormalDataView;
