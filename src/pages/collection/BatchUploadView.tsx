// Input: React、lucide-react 图标、本地模块 batchUploadApi、useFondsStore
// Output: React 组件 BatchUploadView
// Pos: src/pages/collection/BatchUploadView.tsx - 批量上传页面

/**
 * 批量上传页面
 *
 * 功能：
 * 1. 创建上传批次
 * 2. 拖拽/选择文件上传
 * 3. 实时进度跟踪
 * 4. 文件列表展示
 * 5. 四性检测触发
 *
 * 符合 GB/T 39362-2020 电子会计档案管理系统建设要求
 */

import React, { useState, useCallback, useRef, useEffect } from 'react';
import {
  Upload as UploadIcon,
  FileText,
  CheckCircle2,
  XCircle,
  AlertCircle,
  Trash2,
  Pause,
  Play,
  ShieldCheck,
  ArrowLeft,
  FolderOpen,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import { message, Upload, Modal, Progress, Button, Select, Form, Input, Card, Row, Col, Statistic, Alert } from 'antd';
import type { UploadProps, UploadFile } from 'antd';
import { batchUploadApi } from '../../api/batchUpload';
import { useFondsStore } from '../../store/useFondsStore';
import { ComplianceAlert } from './components/ComplianceAlert';
import { ARCHIVE_CATEGORIES, CATEGORY_OPTIONS } from '../../constants/archivalCategories';

/**
 * API 错误类型（axios 错误对象）
 */
interface ApiError {
  response?: {
    status: number;
    data?: {
      message?: string;
      code?: number;
    };
  };
  message?: string;
}

const { Dragger } = Upload;

// ===== Types =====

interface FileUploadItem {
  uid: string;
  file: File;
  status: 'pending' | 'uploading' | 'uploaded' | 'failed' | 'duplicate';
  progress: number;
  error?: string;
  fileId?: string;
}

interface BatchFormData {
  batchName: string;
  fondsCode: string;
  fiscalYear: string;
  fiscalPeriod?: string;
  archivalCategory: 'VOUCHER' | 'LEDGER' | 'REPORT' | 'OTHER';
}

/**
 * 检测结果统计
 */
interface CheckResult {
  passed: number;
  failed: number;
  failedList: Array<{ fileId: string; fileName: string; reason: string }>;
}

export const BatchUploadView: React.FC = () => {
  const navigate = useNavigate();
  const currentFonds = useFondsStore((state) => state.currentFonds);
  const _fileInputRef = useRef<HTMLInputElement>(null); // 预留的文件输入引用

  // ===== State =====
  const [step, setStep] = useState<'create' | 'upload' | 'complete'>('create');
  const [batchInfo, setBatchInfo] = useState<{
    batchId: number;
    batchNo: string;
    batchName: string;
  } | null>(null);
  const [uploadQueue, setUploadQueue] = useState<FileUploadItem[]>([]);
  const [isPaused, setIsPaused] = useState(false);
  const [uploadingIndex, setUploadingIndex] = useState<number>(-1);
  const [selectedCategory, setSelectedCategory] = useState<string>('VOUCHER');
  const [checkResult, setCheckResult] = useState<CheckResult>({ passed: 0, failed: 0, failedList: [] });
  const [form] = Form.useForm<BatchFormData>();

  // ===== Queries =====

  // 轮询批次状态
  const { data: batchDetail } = useQuery({
    queryKey: ['batchDetail', batchInfo?.batchId],
    queryFn: () => batchUploadApi.getBatchDetail(batchInfo!.batchId),
    enabled: !!batchInfo?.batchId && step !== 'create',
    refetchInterval: 2000,
  });

  // 获取文件列表
  const { data: batchFiles = [] } = useQuery({
    queryKey: ['batchFiles', batchInfo?.batchId],
    queryFn: () => batchUploadApi.getBatchFiles(batchInfo!.batchId),
    enabled: !!batchInfo?.batchId && step !== 'create',
    refetchInterval: 3000,
  });

  // ===== Mutations =====

  const createBatchMutation = useMutation({
    mutationFn: (data: BatchFormData) =>
      batchUploadApi.createBatch({
        ...data,
        fondsCode: currentFonds?.fondsCode || data.fondsCode,
        totalFiles: uploadQueue.length,
        autoCheck: true,
      }),
    onSuccess: (response) => {
      setBatchInfo({
        batchId: response.batchId,
        batchNo: response.batchNo,
        batchName: form.getFieldValue('batchName'),
      });
      setStep('upload');
      message.success('批次创建成功');
    },
    onError: (err: ApiError) => {
      if (err?.response?.status === 409) {
        message.error('批次创建失败：批次名称可能已存在，请尝试使用其他名称。');
      } else {
        message.error('批次创建失败: ' + (err.message || '未知错误'));
      }
    },
  });

  const uploadFileMutation = useMutation({
    mutationFn: ({ file, index }: { file: File; index: number }) =>
      batchUploadApi.uploadFile(
        batchInfo!.batchId,
        file,
        (progress) => {
          setUploadQueue((prev) => {
            const updated = [...prev];
            updated[index].progress = progress;
            return updated;
          });
        }
      ),
    onSuccess: (result, variables) => {
      setUploadQueue((prev) => {
        const updated = [...prev];
        updated[variables.index].status =
          result.status === 'UPLOADED' ? 'uploaded' : 'failed';
        updated[variables.index].fileId = result.fileId;
        updated[variables.index].error = result.errorMessage;
        return updated;
      });
    },
    onError: (err: Error, variables) => {
      setUploadQueue((prev) => {
        const updated = [...prev];
        updated[variables.index].status = 'failed';
        updated[variables.index].error = err.message || '上传失败';
        return updated;
      });
    },
  });

  const completeBatchMutation = useMutation({
    mutationFn: () => batchUploadApi.completeBatch(batchInfo!.batchId),
    onSuccess: (data) => {
      setStep('complete');
      // 设置检测结果
      setCheckResult({
        passed: data.passedFiles || 0,
        failed: data.failedFileList?.length || 0,
        failedList: data.failedFileList || []
      });

      // 根据检测结果显示不同消息
      const failedCount = data.failedFileList?.length || 0;
      const passedCount = data.passedFiles || 0;
      if (failedCount > 0) {
        message.warning(`批次完成：${passedCount} 个文件检测通过，${failedCount} 个文件检测失败`);
      } else {
        message.success(`批次完成：所有 ${passedCount} 个文件检测通过`);
      }
    },
  });

  const cancelBatchMutation = useMutation({
    mutationFn: () => batchUploadApi.cancelBatch(batchInfo!.batchId),
    onSuccess: () => {
      message.success('批次已取消');
      navigate('/system/collection');
    },
  });

  const runCheckMutation = useMutation({
    mutationFn: () => batchUploadApi.runFourNatureCheck(batchInfo!.batchId),
    onSuccess: (result) => {
      message.success(result.summary);
    },
  });

  // ===== Effects =====

  // 自动处理上传队列
  useEffect(() => {
    if (step === 'upload' && !isPaused && uploadingIndex === -1) {
      const nextIndex = uploadQueue.findIndex((item) => item.status === 'pending');
      if (nextIndex !== -1) {
        setUploadingIndex(nextIndex);
        setUploadQueue((prev) => {
          const updated = [...prev];
          updated[nextIndex].status = 'uploading';
          return updated;
        });
      }
    }
  }, [step, isPaused, uploadQueue, uploadingIndex]);

  // 执行上传
  useEffect(() => {
    if (uploadingIndex >= 0 && uploadQueue[uploadingIndex]?.status === 'uploading') {
      const item = uploadQueue[uploadingIndex];
      uploadFileMutation.mutate(
        { file: item.file, index: uploadingIndex },
        {
          onSettled: () => {
            setUploadingIndex(-1);
          },
        }
      );
    }
  }, [uploadingIndex, uploadQueue, uploadFileMutation]);

  // ===== Handlers =====

  const handleCreateBatch = useCallback(async () => {
    try {
      const values = await form.validateFields();
      createBatchMutation.mutate(values);
    } catch (_error) {
      message.error('请填写完整的批次信息');
    }
  }, [form, createBatchMutation]);

  const handleFileSelect: UploadProps['onChange'] = useCallback((info: { fileList: UploadFile[] }) => {
    const newFiles = info.fileList
      .filter((file: UploadFile) => file.originFileObj)
      .map((file: UploadFile) => ({
        uid: file.uid,
        file: file.originFileObj as File,
        status: 'pending' as const,
        progress: 0,
      }));

    // 去重：基于文件名和大小判断是否重复
    setUploadQueue((prev) => {
      const existingKeySet = new Set(
        prev.map((item) => `${item.file.name}-${item.file.size}`)
      );

      const filtered = newFiles.filter((item) => {
        const key = `${item.file.name}-${item.file.size}`;
        if (existingKeySet.has(key)) {
          return false; // 跳过重复文件
        }
        existingKeySet.add(key);
        return true;
      });

      return [...prev, ...filtered];
    });
  }, []);

  const handleRemoveFile = useCallback((uid: string) => {
    setUploadQueue((prev) => prev.filter((item) => item.uid !== uid));
  }, []);

  const handleRetryFile = useCallback((uid: string) => {
    setUploadQueue((prev) =>
      prev.map((item) => {
        if (item.uid === uid) {
          return { ...item, status: 'pending' as const, progress: 0, error: undefined };
        }
        return item;
      })
    );
  }, []);

  const handleCompleteBatch = useCallback(() => {
    completeBatchMutation.mutate();
  }, [completeBatchMutation]);

  const handleCancelBatch = useCallback(() => {
    Modal.confirm({
      title: '确认取消',
      content: '取消后将删除所有已上传的文件，是否继续？',
      onOk: () => cancelBatchMutation.mutate(),
    });
  }, [cancelBatchMutation]);

  const handleRunCheck = useCallback(() => {
    runCheckMutation.mutate();
  }, [runCheckMutation]);

  // ===== Render Helpers =====

  const renderFileIcon = (status: FileUploadItem['status']) => {
    switch (status) {
      case 'uploaded':
        return <CheckCircle2 className="text-green-500" size={20} />;
      case 'failed':
        return <XCircle className="text-red-500" size={20} />;
      case 'duplicate':
        return <AlertCircle className="text-amber-500" size={20} />;
      case 'uploading':
        return <UploadIcon className="text-blue-500 animate-pulse" size={20} />;
      default:
        return <FileText className="text-slate-400" size={20} />;
    }
  };

  const getStatusText = (status: FileUploadItem['status'] | string) => {
    const statusMap: Record<string, string> = {
      pending: '等待中',
      uploading: '上传中',
      uploaded: '已上传',
      failed: '上传失败',
      duplicate: '重复文件',
      UPLOADED: '已上传',
      VALIDATED: '已校验',
      FAILED: '失败',
      CHECK_FAILED: '检测失败',
      PENDING: '等待中',
    };
    return statusMap[status] || status;
  };

  const stats = React.useMemo(() => {
    // 优先使用服务器返回的批次详情数据（在步骤2和步骤3）
    if (batchDetail) {
      return {
        total: batchDetail.totalFiles || 0,
        uploaded: batchDetail.uploadedFiles || 0,
        failed: batchDetail.failedFiles || 0,
        duplicate: 0, // 批次详情中没有重复计数，可从 batchFiles 计算
      };
    }
    // 回退到本地队列（仅用于步骤1创建阶段）
    const total = uploadQueue.length;
    const uploaded = uploadQueue.filter((f) => f.status === 'uploaded').length;
    const failed = uploadQueue.filter((f) => f.status === 'failed').length;
    const duplicate = uploadQueue.filter((f) => f.status === 'duplicate').length;
    return { total, uploaded, failed, duplicate };
  }, [batchDetail, uploadQueue]);

  // ===== Render =====

  if (step === 'create') {
    return (
      <div className="p-8 max-w-4xl mx-auto animate-in fade-in duration-300">
        {/* Header */}
        <div className="flex items-center gap-4 mb-8">
          <button
            onClick={() => navigate('/system/collection')}
            className="p-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800"
          >
            <ArrowLeft size={20} />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-slate-900">批量上传</h1>
            <p className="text-slate-500 text-sm">创建上传批次并上传会计档案文件</p>
          </div>
        </div>

        {/* Batch Form */}
        <Card className="mb-6">
          <Form
            form={form}
            layout="vertical"
            initialValues={{
              fondsCode: currentFonds?.fondsCode || '001',
              fiscalYear: new Date().getFullYear().toString(),
              archivalCategory: 'VOUCHER',
            }}
          >
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  label="批次名称"
                  name="batchName"
                  rules={[{ required: true, message: '请输入批次名称' }]}
                >
                  <Input placeholder="例如：2024年1月凭证" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  label="全宗代码"
                  name="fondsCode"
                  rules={[{ required: true, message: '请输入全宗代码' }]}
                >
                  <Input placeholder="001" />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={16}>
              <Col span={8}>
                <Form.Item
                  label="会计年度"
                  name="fiscalYear"
                  rules={[{ required: true, message: '请输入会计年度' }]}
                >
                  <Input placeholder="2024" />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item label="会计期间" name="fiscalPeriod">
                  <Select
                    placeholder="选择期间"
                    allowClear
                    options={[
                      { label: '1月', value: '01' },
                      { label: '2月', value: '02' },
                      { label: '3月', value: '03' },
                      { label: '4月', value: '04' },
                      { label: '5月', value: '05' },
                      { label: '6月', value: '06' },
                      { label: '7月', value: '07' },
                      { label: '8月', value: '08' },
                      { label: '9月', value: '09' },
                      { label: '10月', value: '10' },
                      { label: '11月', value: '11' },
                      { label: '12月', value: '12' },
                    ]}
                  />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item
                  label="档案门类"
                  name="archivalCategory"
                  rules={[{ required: true, message: '请选择档案门类' }]}
                >
                  <Select
                    onChange={(val) => setSelectedCategory(val)}
                    options={CATEGORY_OPTIONS.map(opt => ({
                      label: (
                        <div className="flex items-center gap-2">
                          <opt.icon size={14} className={`text-${opt.color}-500`} />
                          <span>{opt.label}</span>
                        </div>
                      ),
                      value: opt.value
                    }))}
                  />
                </Form.Item>
              </Col>
            </Row>

            {/* 显性强校验提示 */}
            {selectedCategory && (
              <div className="mt-4 animate-in slide-in-from-top-2 duration-300">
                <Alert
                  type="info"
                  showIcon
                  icon={React.createElement((ARCHIVE_CATEGORIES as any)[selectedCategory]?.icon || AlertCircle, { size: 18 })}
                  title={`${(ARCHIVE_CATEGORIES as any)[selectedCategory]?.label} 合规说明`}
                  description={
                    <div className="space-y-1">
                      <p className="text-sm font-medium">{(ARCHIVE_CATEGORIES as any)[selectedCategory]?.description}</p>
                      <p className="text-xs text-slate-500 bg-slate-100 p-2 rounded border-l-4 border-blue-500">
                        校验规则：{(ARCHIVE_CATEGORIES as any)[selectedCategory]?.validationRule}
                      </p>
                    </div>
                  }
                />
              </div>
            )}
          </Form>
        </Card>

        {/* File Upload Area */}
        <Card className="mb-6">
          <Dragger
            multiple
            accept=".pdf,.ofd,.xml,.jpg,.jpeg,.png,.tif,.tiff"
            customRequest={() => { }}
            onChange={handleFileSelect}
            showUploadList={false}
          >
            <p className="ant-upload-drag-icon">
              <FolderOpen size={48} className="text-blue-500" />
            </p>
            <p className="text-lg font-medium text-slate-700">
              点击或拖拽文件到此区域上传
            </p>
            <p className="text-slate-500 text-sm mt-2">
              支持 PDF、OFD、XML、JPG、PNG、TIFF 格式，单个文件不超过 100MB
            </p>
          </Dragger>
        </Card>

        {/* File List */}
        {uploadQueue.length > 0 && (
          <Card className="mb-6">
            <div className="flex justify-between items-center mb-4">
              <h3 className="font-semibold text-slate-800">
                已选择 {uploadQueue.length} 个文件
              </h3>
              <Button
                danger
                size="small"
                onClick={() => setUploadQueue([])}
              >
                清空列表
              </Button>
            </div>

            <div className="space-y-2 max-h-64 overflow-y-auto">
              {uploadQueue.map((item) => (
                <div
                  key={item.uid}
                  className="flex items-center gap-3 p-3 bg-slate-50 dark:bg-slate-800 rounded-lg"
                >
                  {renderFileIcon(item.status)}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-slate-700 truncate">
                      {item.file.name}
                    </p>
                    <p className="text-xs text-slate-500">
                      {(item.file.size / 1024 / 1024).toFixed(2)} MB
                      {item.status === 'uploading' && ` - ${item.progress}%`}
                    </p>
                    {item.error && (
                      <p className="text-xs text-red-500">{item.error}</p>
                    )}
                  </div>
                  {item.status === 'failed' && (
                    <Button size="small" onClick={() => handleRetryFile(item.uid)}>
                      重试
                    </Button>
                  )}
                  <Button
                    size="small"
                    danger
                    disabled={item.status === 'uploading'}
                    onClick={() => handleRemoveFile(item.uid)}
                  >
                    <Trash2 size={14} />
                  </Button>
                </div>
              ))}
            </div>
          </Card>
        )}

        {/* Actions */}
        <div className="flex justify-end gap-3">
          <Button onClick={() => navigate('/system/collection')}>取消</Button>
          <Button
            type="primary"
            onClick={handleCreateBatch}
            disabled={uploadQueue.length === 0}
            loading={createBatchMutation.isPending}
          >
            开始上传 ({uploadQueue.length})
          </Button>
        </div>
      </div>
    );
  }

  // Upload & Complete Steps
  return (
    <div className="p-8 max-w-6xl mx-auto animate-in fade-in duration-300">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/system/collection')}
            className="p-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800"
          >
            <ArrowLeft size={20} />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-slate-900">
              {batchInfo?.batchName}
            </h1>
            <p className="text-slate-500 text-sm">
              批次号: {batchInfo?.batchNo} · 状态: {batchDetail?.status}
            </p>
          </div>
        </div>

        {step === 'upload' && (
          <div className="flex items-center gap-2">
            <Button
              icon={isPaused ? <Play size={16} /> : <Pause size={16} />}
              onClick={() => setIsPaused(!isPaused)}
            >
              {isPaused ? '继续' : '暂停'}
            </Button>
            <Button
              type="primary"
              onClick={handleCompleteBatch}
              disabled={stats.uploaded === 0}
              loading={completeBatchMutation.isPending}
            >
              {completeBatchMutation.isPending ? '正在执行四性检测...' : '完成上传'}
            </Button>
          </div>
        )}
      </div>

      {/* Statistics */}
      <Row gutter={16} className="mb-6">
        <Col span={6}>
          <Card>
            <Statistic title="总文件数" value={stats.total} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="已上传"
              value={stats.uploaded}
              styles={{ content: { color: '#3f8600' } }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="失败"
              value={stats.failed}
              styles={{ content: { color: stats.failed > 0 ? '#cf1322' : undefined } }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="重复"
              value={stats.duplicate}
              styles={{ content: { color: stats.duplicate > 0 ? '#faad14' : undefined } }}
            />
          </Card>
        </Col>
      </Row>

      {/* Compliance Alert */}
      <ComplianceAlert category={batchInfo ? form.getFieldValue('archivalCategory') : selectedCategory} className="mb-6" />

      {/* Overall Progress */}
      {step === 'upload' && (
        <Card className="mb-6">
          <div className="flex items-center gap-4">
            <div className="flex-1">
              <div className="flex justify-between mb-2">
                <span className="text-sm font-medium">上传进度</span>
                <span className="text-sm text-slate-500">
                  {stats.uploaded} / {stats.total}
                </span>
              </div>
              <Progress
                percent={stats.total > 0 ? Math.round((stats.uploaded / stats.total) * 100) : 0}
                status={stats.failed > 0 ? 'exception' : 'active'}
              />
            </div>
          </div>
        </Card>
      )}

      {/* File List (from server) */}
      <Card title="文件列表">
        <div className="space-y-2">
          {batchFiles.map((file) => (
            <div
              key={file.id}
              className="flex items-center gap-3 p-3 bg-slate-50 dark:bg-slate-800 rounded-lg"
            >
              {renderFileIcon(
                file.uploadStatus === 'UPLOADED' || file.uploadStatus === 'VALIDATED'
                  ? 'uploaded'
                  : file.uploadStatus === 'FAILED' || file.uploadStatus === 'CHECK_FAILED'
                    ? 'failed'
                    : 'pending'
              )}
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-slate-700 truncate">
                  {file.originalFilename}
                </p>
                <p className="text-xs text-slate-500">
                  {((file.fileSizeBytes || 0) / 1024 / 1024).toFixed(2)} MB
                  {' · '}{getStatusText(file.uploadStatus)}
                </p>
                {file.errorMessage && (
                  <p className="text-xs text-red-500">{file.errorMessage}</p>
                )}
              </div>
            </div>
          ))}
        </div>
      </Card>

      {/* Actions for Complete Step */}
      {step === 'complete' && (
        <div className="mt-6">
          {/* Completion Summary with Check Results */}
          <Alert
            type={checkResult.failed > 0 ? 'warning' : 'success'}
            showIcon
            className="mb-4"
            title={checkResult.failed > 0 ? '上传完成（部分文件检测失败）' : '上传完成！'}
            description={
              checkResult.failed > 0
                ? `${checkResult.passed} 个文件检测通过，${checkResult.failed} 个文件检测失败。元数据已根据批次信息初步生成，失败的文件需要处理后才能归档。`
                : `所有 ${checkResult.passed} 个文件均已通过四性检测，元数据已根据批次信息智能补全，可以提交归档。`
            }
          />

          {/* Failed Files List */}
          {checkResult.failed > 0 && checkResult.failedList.length > 0 && (
            <Card className="mb-4" title="检测失败文件">
              <div className="space-y-2">
                {checkResult.failedList.map((item, index) => (
                  <div key={index} className="flex items-center justify-between p-2 bg-red-50 dark:bg-red-900/20 rounded">
                    <span className="text-sm font-medium">{item.fileName}</span>
                    <span className="text-xs text-red-600 dark:text-red-400">{item.reason}</span>
                  </div>
                ))}
              </div>
            </Card>
          )}

          {/* Action Buttons */}
          <div className="flex justify-center gap-4">
            <Button
              type="primary"
              onClick={() => navigate('/system/pre-archive')}
            >
              前往预归档库
            </Button>
            <Button onClick={() => navigate('/system/collection/upload')}>
              继续上传
            </Button>
            <Button danger onClick={handleCancelBatch}>
              取消批次
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};

export default BatchUploadView;
