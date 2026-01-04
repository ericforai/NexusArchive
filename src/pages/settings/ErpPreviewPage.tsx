// Input: React, erpApi
// Output: ErpPreviewPage component
// Pos: 系统设置 - ERP AI 适配器预览确认页面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Upload, FileText, CheckCircle, AlertCircle, ChevronRight,
    Loader2, PlusCircle, Database, Zap, Settings, ArrowLeft,
    Download, Eye
} from 'lucide-react';
import { erpApi, PreviewResponse, ScenarioPreviewItem, ExistingConfigItem } from '../../api/erp';
import { toast } from 'react-hot-toast';

// Adapter icons (matching IntegrationSettings)
const ADAPTER_ICONS: Record<string, React.ReactNode> = {
    YONSUITE: <Database size={20} />,
    KINGDEE: <Zap size={20} />,
    WEAVER: <Settings size={20} />,
    GENERIC: <Settings size={20} />
};

const ErpPreviewPage: React.FC = () => {
    const navigate = useNavigate();

    // State
    const [file, setFile] = useState<File | null>(null);
    const [loading, setLoading] = useState(false);
    const [previewData, setPreviewData] = useState<PreviewResponse | null>(null);
    const [selectedConfigId, setSelectedConfigId] = useState<number | null>(null);
    const [deploying, setDeploying] = useState(false);

    // Handle file selection
    const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const selectedFile = e.target.files?.[0];
        if (selectedFile) {
            setFile(selectedFile);
            setPreviewData(null);
            setSelectedConfigId(null);
        }
    };

    // Handle preview
    const handlePreview = async () => {
        if (!file) {
            toast.error('请先选择文件');
            return;
        }

        setLoading(true);
        try {
            const response = await erpApi.previewScenarios(file);
            if (response.success === true && response.data) {
                setPreviewData(response.data);
                // Auto-select suggested config
                if (response.data.suggestedConfigId) {
                    setSelectedConfigId(response.data.suggestedConfigId);
                }
                toast.success('识别成功');
            } else {
                toast.error(response.message || '识别失败');
            }
        } catch (error) {
            console.error('Preview failed:', error);
            toast.error('识别失败，请检查文件格式');
        } finally {
            setLoading(false);
        }
    };

    // Handle deploy
    const handleDeploy = async () => {
        if (!file || !previewData) {
            toast.error('请先预览');
            return;
        }

        setDeploying(true);
        try {
            // TODO: 调用实际的部署接口
            // 目前先显示提示信息
            if (selectedConfigId) {
                toast.success(`将添加到配置 ID: ${selectedConfigId}`);
            } else {
                toast.success('将创建新配置');
            }
            // TODO: implement actual deploy call
        } catch (error) {
            console.error('Deploy failed:', error);
            toast.error('部署失败');
        } finally {
            setDeploying(false);
        }
    };

    // Render file upload section
    const renderFileUpload = () => (
        <div className="bg-white rounded-lg border border-gray-200 p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <Upload size={20} />
                上传 OpenAPI 文档
            </h2>

            <div className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center hover:border-blue-400 transition-colors">
                <input
                    type="file"
                    accept=".json,.yaml,.yml"
                    onChange={handleFileSelect}
                    className="hidden"
                    id="file-upload"
                />
                <label
                    htmlFor="file-upload"
                    className="cursor-pointer flex flex-col items-center gap-3"
                >
                    <FileText size={40} className="text-gray-400" />
                    <div>
                        <p className="text-gray-700 font-medium">点击选择文件</p>
                        <p className="text-gray-500 text-sm mt-1">支持 OpenAPI JSON/YAML 格式</p>
                    </div>
                </label>
            </div>

            {file && (
                <div className="mt-4 flex items-center justify-between bg-gray-50 rounded-lg p-3">
                    <div className="flex items-center gap-2">
                        <FileText size={18} className="text-blue-600" />
                        <span className="text-sm font-medium text-gray-700">{file.name}</span>
                        <span className="text-xs text-gray-500">({(file.size / 1024).toFixed(1)} KB)</span>
                    </div>
                    <button
                        onClick={() => {
                            setFile(null);
                            setPreviewData(null);
                            setSelectedConfigId(null);
                        }}
                        className="text-red-600 hover:text-red-700 text-sm"
                    >
                        移除
                    </button>
                </div>
            )}

            <div className="mt-4 flex gap-3">
                <button
                    onClick={handlePreview}
                    disabled={!file || loading}
                    className="flex-1 bg-blue-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                >
                    {loading ? (
                        <>
                            <Loader2 size={18} className="animate-spin" />
                            识别中...
                        </>
                    ) : (
                        <>
                            <Eye size={18} />
                            识别场景
                        </>
                    )}
                </button>
            </div>
        </div>
    );

    // Render preview results
    const renderPreview = () => {
        if (!previewData) return null;

        return (
            <div className="space-y-6">
                {/* ERP Type */}
                <div className="bg-white rounded-lg border border-gray-200 p-6">
                    <h3 className="text-lg font-semibold text-gray-900 mb-3">识别结果</h3>
                    <div className="flex items-center gap-3">
                        <div className="p-2 bg-blue-50 text-blue-600 rounded-lg">
                            {ADAPTER_ICONS[previewData.erpType] || ADAPTER_ICONS.GENERIC}
                        </div>
                        <div>
                            <p className="font-medium text-gray-900">{previewData.erpDisplayName}</p>
                            <p className="text-sm text-gray-500">识别到 {previewData.scenarios.length} 个场景</p>
                        </div>
                    </div>
                </div>

                {/* Scenarios */}
                <div className="bg-white rounded-lg border border-gray-200 p-6">
                    <h3 className="text-lg font-semibold text-gray-900 mb-3">识别的场景</h3>
                    <div className="space-y-3">
                        {previewData.scenarios.map((scenario, index) => (
                            <div
                                key={index}
                                className="border border-gray-200 rounded-lg p-4 hover:border-blue-300 transition-colors"
                            >
                                <div className="flex items-start justify-between">
                                    <div className="flex-1">
                                        <div className="flex items-center gap-2 mb-1">
                                            <CheckCircle size={16} className="text-green-600" />
                                            <span className="font-medium text-gray-900">{scenario.displayName}</span>
                                        </div>
                                        <p className="text-sm text-gray-600 ml-6">{scenario.description}</p>
                                        <div className="mt-2 flex items-center gap-4 text-xs text-gray-500 ml-6">
                                            <span className="font-mono bg-gray-100 px-2 py-1 rounded">
                                                {scenario.scenarioKey}
                                            </span>
                                            <span>{scenario.method}</span>
                                            <span className="truncate max-w-xs">{scenario.apiPath}</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Existing Configs */}
                {previewData.existingConfigs.length > 0 && (
                    <div className="bg-white rounded-lg border border-gray-200 p-6">
                        <h3 className="text-lg font-semibold text-gray-900 mb-3">选择目标连接器</h3>
                        <div className="space-y-3">
                            <label className="flex items-center gap-3 p-3 border border-gray-200 rounded-lg hover:border-blue-300 transition-colors cursor-pointer">
                                <input
                                    type="radio"
                                    name="config"
                                    checked={selectedConfigId === null}
                                    onChange={() => setSelectedConfigId(null)}
                                    className="text-blue-600"
                                />
                                <PlusCircle size={20} className="text-green-600" />
                                <div className="flex-1">
                                    <p className="font-medium text-gray-900">创建新连接器</p>
                                    <p className="text-sm text-gray-500">添加为新的 {previewData.erpDisplayName} 连接器</p>
                                </div>
                            </label>

                            {previewData.existingConfigs.map(config => (
                                <label
                                    key={config.configId}
                                    className="flex items-center gap-3 p-3 border border-gray-200 rounded-lg hover:border-blue-300 transition-colors cursor-pointer"
                                >
                                    <input
                                        type="radio"
                                        name="config"
                                        checked={selectedConfigId === config.configId}
                                        onChange={() => setSelectedConfigId(config.configId)}
                                        className="text-blue-600"
                                    />
                                    <div className="flex-1">
                                        <div className="flex items-center gap-2">
                                            <Database size={18} className="text-blue-600" />
                                            <span className="font-medium text-gray-900">{config.name}</span>
                                            {previewData.suggestedConfigId === config.configId && (
                                                <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded">
                                                    推荐
                                                </span>
                                            )}
                                        </div>
                                        {config.baseUrl && (
                                            <p className="text-sm text-gray-500 ml-6">{config.baseUrl}</p>
                                        )}
                                    </div>
                                </label>
                            ))}
                        </div>
                    </div>
                )}

                {/* Actions */}
                <div className="flex gap-3">
                    <button
                        onClick={handleDeploy}
                        disabled={deploying}
                        className="flex-1 bg-green-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-green-700 disabled:bg-gray-300 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                    >
                        {deploying ? (
                            <>
                                <Loader2 size={18} className="animate-spin" />
                                部署中...
                            </>
                        ) : (
                            <>
                                <Download size={18} />
                                确认部署
                            </>
                        )}
                    </button>
                    <button
                        onClick={() => {
                            setPreviewData(null);
                            setSelectedConfigId(null);
                        }}
                        className="px-4 py-2 border border-gray-300 rounded-lg font-medium hover:bg-gray-50"
                    >
                        重新识别
                    </button>
                </div>
            </div>
        );
    };

    return (
        <div className="min-h-screen bg-gray-50 p-6">
            <div className="max-w-4xl mx-auto">
                {/* Header */}
                <div className="mb-6">
                    <button
                        onClick={() => navigate(-1)}
                        className="flex items-center gap-2 text-gray-600 hover:text-gray-900 mb-4"
                    >
                        <ArrowLeft size={18} />
                        返回
                    </button>
                    <h1 className="text-2xl font-bold text-gray-900">ERP AI 适配器预览</h1>
                    <p className="text-gray-600 mt-1">
                        上传 OpenAPI 文档，智能识别场景并部署到 ERP 连接器
                    </p>
                </div>

                {/* Content */}
                {renderFileUpload()}
                {renderPreview()}
            </div>
        </div>
    );
};

export default ErpPreviewPage;
