// Input: React、react-router-dom、Ant Design、本地模块 voucher、archivesApi、documentsApi
// Output: React 组件 ArchiveDetailPage（含档案详情与文档协作版本面板）
// Pos: src/pages/archives/ArchiveDetailPage.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, FileText } from 'lucide-react';
import { Alert, Breadcrumb, Button, Input, Tabs, message } from 'antd';
// VoucherDTO 类型由 useVoucherData hook 内部使用
import { VoucherMetadata, VoucherPreviewCanvas, OriginalDocumentPreview } from '../../components/voucher';
import { useVoucherData } from './hooks/useVoucherData';
import { archivesApi } from '../../api/archives';
import { documentsApi, type DocumentSection, type DocumentVersion } from '../../api/documents';

type TabKey = 'metadata' | 'voucher' | 'attachments' | 'workflow';

interface WorkflowPanelProps {
  archiveId: string;
  archiveTitle?: string;
}

const DEFAULT_SECTION_SUFFIX = 'main';

function createSectionId(archiveId: string) {
  return `${archiveId}:${DEFAULT_SECTION_SUFFIX}`;
}

const DocumentWorkflowPanel: React.FC<WorkflowPanelProps> = ({ archiveId, archiveTitle }) => {
  const sectionId = React.useMemo(() => createSectionId(archiveId), [archiveId]);
  const [section, setSection] = React.useState<DocumentSection | null>(null);
  const [versions, setVersions] = React.useState<DocumentVersion[]>([]);
  const [sectionLoading, setSectionLoading] = React.useState(false);
  const [versionsLoading, setVersionsLoading] = React.useState(false);
  const [savingSection, setSavingSection] = React.useState(false);
  const [submittingAssignment, setSubmittingAssignment] = React.useState(false);
  const [submittingLock, setSubmittingLock] = React.useState(false);
  const [submittingReminder, setSubmittingReminder] = React.useState(false);
  const [submittingVersion, setSubmittingVersion] = React.useState(false);
  const [rollingBackVersionId, setRollingBackVersionId] = React.useState<string | null>(null);
  const [sectionError, setSectionError] = React.useState<string | null>(null);
  const [title, setTitle] = React.useState('');
  const [content, setContent] = React.useState('');
  const [assigneeId, setAssigneeId] = React.useState('');
  const [assigneeName, setAssigneeName] = React.useState('');
  const [assignmentNote, setAssignmentNote] = React.useState('');
  const [lockReason, setLockReason] = React.useState('');
  const [reminderMessage, setReminderMessage] = React.useState('');
  const [reminderAt, setReminderAt] = React.useState('');
  const [reminderRecipientId, setReminderRecipientId] = React.useState('');
  const [reminderRecipientName, setReminderRecipientName] = React.useState('');
  const [versionName, setVersionName] = React.useState('');
  const [versionDescription, setVersionDescription] = React.useState('');

  const syncSectionState = React.useCallback((nextSection: DocumentSection | null) => {
    setSection(nextSection);
    setTitle(nextSection?.title || archiveTitle || '文档主稿');
    setContent(nextSection?.content || '');
    setAssigneeId(nextSection?.assignment?.assigneeId || '');
    setAssigneeName(nextSection?.assignment?.assigneeName || '');
    setAssignmentNote(nextSection?.assignment?.note || '');
    setLockReason(nextSection?.lock?.reason || '');
    const latestReminder = nextSection?.reminders?.[0];
    setReminderMessage(latestReminder?.message || '');
    setReminderAt(latestReminder?.remindAt ? String(latestReminder.remindAt).slice(0, 16) : '');
    setReminderRecipientId(latestReminder?.recipientId || '');
    setReminderRecipientName(latestReminder?.recipientName || '');
  }, [archiveTitle]);

  const loadSection = React.useCallback(async () => {
    setSectionLoading(true);
    setSectionError(null);
    try {
      const response = await documentsApi.getSection(archiveId, sectionId);
      syncSectionState(response.data?.data ?? null);
    } catch (error: any) {
      if (error?.response?.status === 404) {
        syncSectionState(null);
        return;
      }
      setSectionError(error?.message || '加载协作数据失败');
    } finally {
      setSectionLoading(false);
    }
  }, [archiveId, sectionId, syncSectionState]);

  const loadVersions = React.useCallback(async () => {
    setVersionsLoading(true);
    try {
      const response = await documentsApi.listVersions(archiveId);
      setVersions(response.data?.data || []);
    } catch (error: any) {
      message.error(error?.message || '加载版本列表失败');
    } finally {
      setVersionsLoading(false);
    }
  }, [archiveId]);

  useEffect(() => {
    loadSection();
    loadVersions();
  }, [loadSection, loadVersions]);

  const refreshWorkflow = React.useCallback(async () => {
    await Promise.all([loadSection(), loadVersions()]);
  }, [loadSection, loadVersions]);

  const handleSaveSection = async () => {
    setSavingSection(true);
    try {
      await documentsApi.updateSection(archiveId, sectionId, {
        title: title.trim() || archiveTitle || '文档主稿',
        content,
        sortOrder: 0,
      });
      await loadSection();
      message.success('文档主稿已保存');
    } catch (error: any) {
      message.error(error?.message || '保存文档主稿失败');
    } finally {
      setSavingSection(false);
    }
  };

  const ensureSectionBeforeWorkflow = async () => {
    if (section) {
      return;
    }
    await documentsApi.updateSection(archiveId, sectionId, {
      title: title.trim() || archiveTitle || '文档主稿',
      content,
      sortOrder: 0,
    });
  };

  const handleAssignSection = async () => {
    if (!assigneeId.trim()) {
      message.warning('请输入负责人 ID');
      return;
    }
    setSubmittingAssignment(true);
    try {
      await ensureSectionBeforeWorkflow();
      await documentsApi.createAssignment(archiveId, {
        sectionId,
        assigneeId: assigneeId.trim(),
        assigneeName: assigneeName.trim() || undefined,
        note: assignmentNote.trim() || undefined,
        active: true,
      });
      await loadSection();
      message.success('分工已保存');
    } catch (error: any) {
      message.error(error?.message || '保存分工失败');
    } finally {
      setSubmittingAssignment(false);
    }
  };

  const handleLockSection = async () => {
    setSubmittingLock(true);
    try {
      await ensureSectionBeforeWorkflow();
      await documentsApi.createLock(archiveId, {
        sectionId,
        reason: lockReason.trim() || undefined,
        active: true,
      });
      await loadSection();
      message.success('锁定状态已保存');
    } catch (error: any) {
      message.error(error?.message || '保存锁定状态失败');
    } finally {
      setSubmittingLock(false);
    }
  };

  const handleCreateReminder = async () => {
    if (!reminderMessage.trim() || !reminderAt.trim() || !reminderRecipientId.trim()) {
      message.warning('请填写提醒内容、提醒时间和接收人 ID');
      return;
    }
    setSubmittingReminder(true);
    try {
      await ensureSectionBeforeWorkflow();
      await documentsApi.createReminder(archiveId, {
        sectionId,
        message: reminderMessage.trim(),
        remindAt: reminderAt,
        recipientId: reminderRecipientId.trim(),
        recipientName: reminderRecipientName.trim() || undefined,
      });
      await loadSection();
      message.success('提醒已创建');
    } catch (error: any) {
      message.error(error?.message || '创建提醒失败');
    } finally {
      setSubmittingReminder(false);
    }
  };

  const handleCreateVersion = async () => {
    if (!versionName.trim()) {
      message.warning('请输入版本名称');
      return;
    }
    setSubmittingVersion(true);
    try {
      await ensureSectionBeforeWorkflow();
      await documentsApi.createVersion(archiveId, {
        versionName: versionName.trim(),
        description: versionDescription.trim() || undefined,
      });
      setVersionName('');
      setVersionDescription('');
      await loadVersions();
      message.success('版本已创建');
    } catch (error: any) {
      message.error(error?.message || '创建版本失败');
    } finally {
      setSubmittingVersion(false);
    }
  };

  const handleRollbackVersion = async (versionId: string) => {
    setRollingBackVersionId(versionId);
    try {
      await documentsApi.rollbackVersion(archiveId, versionId);
      await refreshWorkflow();
      message.success('版本已回滚');
    } catch (error: any) {
      message.error(error?.message || '版本回滚失败');
    } finally {
      setRollingBackVersionId(null);
    }
  };

  return (
    <div className="p-6 space-y-6" data-testid="document-workflow-panel">
      {sectionError && (
        <Alert
          type="error"
          message="协作数据加载失败"
          description={sectionError}
          showIcon
        />
      )}

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        <div className="border border-slate-200 rounded-xl p-4 space-y-4">
          <div>
            <h3 className="text-base font-semibold text-slate-900">文档主稿</h3>
            <p className="text-sm text-slate-500">当前以档案详情页作为真实接入面，刷新后会从后端恢复协作状态。</p>
          </div>
          <label className="block text-sm font-medium text-slate-700">
            标题
            <Input
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="输入章节标题"
            />
          </label>
          <label className="block text-sm font-medium text-slate-700">
            内容
            <Input.TextArea
              value={content}
              onChange={(event) => setContent(event.target.value)}
              rows={8}
              placeholder="输入文档主稿内容"
            />
          </label>
          <div className="flex items-center justify-between gap-3">
            <span className="text-xs text-slate-400">
              {sectionLoading ? '正在加载后端状态...' : `Section ID: ${sectionId}`}
            </span>
            <Button type="primary" onClick={handleSaveSection} loading={savingSection}>
              保存主稿
            </Button>
          </div>
        </div>

        <div className="border border-slate-200 rounded-xl p-4 space-y-4">
          <div>
            <h3 className="text-base font-semibold text-slate-900">协作状态</h3>
            <p className="text-sm text-slate-500">分工、锁定和提醒均持久化到后端。</p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <label className="block text-sm font-medium text-slate-700">
              负责人 ID
              <Input
                value={assigneeId}
                onChange={(event) => setAssigneeId(event.target.value)}
                placeholder="例如 user-2"
              />
            </label>
            <label className="block text-sm font-medium text-slate-700">
              负责人名称
              <Input
                value={assigneeName}
                onChange={(event) => setAssigneeName(event.target.value)}
                placeholder="例如 张三"
              />
            </label>
          </div>
          <label className="block text-sm font-medium text-slate-700">
            分工备注
            <Input
              value={assignmentNote}
              onChange={(event) => setAssignmentNote(event.target.value)}
              placeholder="补充说明"
            />
          </label>
          <Button onClick={handleAssignSection} loading={submittingAssignment}>
            保存分工
          </Button>

          <label className="block text-sm font-medium text-slate-700">
            锁定原因
            <Input
              value={lockReason}
              onChange={(event) => setLockReason(event.target.value)}
              placeholder="例如 正在校对"
            />
          </label>
          <Button onClick={handleLockSection} loading={submittingLock}>
            保存锁定
          </Button>

          <div className="pt-2 border-t border-slate-100 space-y-3">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <label className="block text-sm font-medium text-slate-700">
                接收人 ID
                <Input
                  value={reminderRecipientId}
                  onChange={(event) => setReminderRecipientId(event.target.value)}
                  placeholder="例如 user-2"
                />
              </label>
              <label className="block text-sm font-medium text-slate-700">
                接收人名称
                <Input
                  value={reminderRecipientName}
                  onChange={(event) => setReminderRecipientName(event.target.value)}
                  placeholder="例如 张三"
                />
              </label>
            </div>
            <label className="block text-sm font-medium text-slate-700">
              提醒内容
              <Input
                value={reminderMessage}
                onChange={(event) => setReminderMessage(event.target.value)}
                placeholder="例如 今天下班前完成复核"
              />
            </label>
            <label className="block text-sm font-medium text-slate-700">
              提醒时间
              <Input
                type="datetime-local"
                value={reminderAt}
                onChange={(event) => setReminderAt(event.target.value)}
              />
            </label>
            <Button onClick={handleCreateReminder} loading={submittingReminder}>
              创建提醒
            </Button>
          </div>
        </div>
      </div>

      <div className="border border-slate-200 rounded-xl p-4 space-y-4">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h3 className="text-base font-semibold text-slate-900">版本管理</h3>
            <p className="text-sm text-slate-500">创建快照后可直接回滚，刷新或重新登录后仍能看到状态。</p>
          </div>
          <Button onClick={refreshWorkflow} loading={sectionLoading || versionsLoading}>
            刷新状态
          </Button>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-[1fr_2fr_auto] gap-3 items-end">
          <label className="block text-sm font-medium text-slate-700">
            版本名称
            <Input
              value={versionName}
              onChange={(event) => setVersionName(event.target.value)}
              placeholder="例如 v1.0"
            />
          </label>
          <label className="block text-sm font-medium text-slate-700">
            版本说明
            <Input
              value={versionDescription}
              onChange={(event) => setVersionDescription(event.target.value)}
              placeholder="例如 完成首轮校对"
            />
          </label>
          <Button type="primary" onClick={handleCreateVersion} loading={submittingVersion}>
            创建版本
          </Button>
        </div>
        <div className="space-y-3">
          {versions.length === 0 ? (
            <div className="text-sm text-slate-400">暂无版本记录</div>
          ) : (
            versions.map((version) => (
              <div
                key={version.id}
                className="flex flex-col md:flex-row md:items-center md:justify-between gap-3 border border-slate-100 rounded-lg p-3"
              >
                <div className="space-y-1">
                  <div className="font-medium text-slate-900">{version.versionName}</div>
                  <div className="text-sm text-slate-500">{version.description || '无说明'}</div>
                  <div className="text-xs text-slate-400">
                    创建人 {version.createdBy || '未知'} · {version.createdAt || '时间未知'}
                  </div>
                </div>
                <Button
                  danger
                  onClick={() => handleRollbackVersion(version.id)}
                  loading={rollingBackVersionId === version.id}
                >
                  回滚到此版本
                </Button>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

export const ArchiveDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = React.useState<TabKey>('metadata');

  // 档案数据状态（从 API 获取）
  const [archiveData, setArchiveData] = React.useState<any>(null);
  const [_archiveLoading, setArchiveLoading] = React.useState(false);

  // 从 API 获取真实档案数据
  useEffect(() => {
    if (!id) return;

    const fetchArchive = async () => {
      setArchiveLoading(true);
      try {
        const response = await archivesApi.getArchiveById(id);
        if (response?.code === 200 && response.data) {
          setArchiveData(response.data);
        } else {
          if (import.meta.env.DEV) console.warn('[ArchiveDetailPage] Archive not found for id:', id);
          setArchiveData(null);
        }
      } catch (error) {
        if (import.meta.env.DEV) console.error('[ArchiveDetailPage] Failed to fetch archive:', error);
        setArchiveData(null);
      } finally {
        setArchiveLoading(false);
      }
    };

    fetchArchive();
  }, [id]);

  // 从 API 数据构建 row 对象
  const row = React.useMemo(() => {
    if (!id) return null;
    if (archiveData) {
      return {
        id: archiveData.id,
        code: archiveData.archiveCode || archiveData.erpVoucherNo || archiveData.id,
        archivalCode: archiveData.archiveCode,
        ...archiveData,
      };
    }
    // Fallback：数据加载中时显示 ID
    return { id, code: id, archivalCode: id };
  }, [id, archiveData]);

  // 使用自定义 hook 获取凭证数据
  const { voucherData, isLoading } = useVoucherData({
    row,
    enabled: !!id,
  });

  const handleBack = () => {
    navigate(-1); // Go back to previous page
  };

  if (!id) {
    return (
      <div className="flex items-center justify-center h-screen">
        <p className="text-slate-500">无效的档案 ID</p>
      </div>
    );
  }

  const tabItems = [
    {
      key: 'metadata',
      label: '业务元数据',
      children: (
        <div className="p-4 overflow-y-auto" style={{ height: 'calc(100vh - 250px)' }}>
          {voucherData ? (
            <VoucherMetadata data={voucherData} />
          ) : (
            <div className="flex items-center justify-center h-full text-slate-400">
              <p>{isLoading ? '加载中...' : '暂无凭证数据'}</p>
            </div>
          )}
        </div>
      ),
    },
    {
      key: 'voucher',
      label: '会计凭证',
      children: (
        <div className="p-4 overflow-y-auto" style={{ height: 'calc(100vh - 250px)' }}>
          {voucherData ? (
            <VoucherPreviewCanvas data={voucherData} />
          ) : (
            <div className="flex items-center justify-center h-full text-slate-400">
              <p>{isLoading ? '加载中...' : '暂无凭证数据'}</p>
            </div>
          )}
        </div>
      ),
    },
    {
      key: 'attachments',
      label: `关联附件${voucherData?.attachments && voucherData.attachments.length > 0 ? ` (${voucherData.attachments.length})` : ''}`,
      children: (
        <div className="p-0 overflow-y-auto" style={{ height: 'calc(100vh - 250px)' }}>
          {isLoading ? (
            <div className="flex items-center justify-center h-full text-slate-400">
              <p>加载中...</p>
            </div>
          ) : voucherData?.attachments && voucherData.attachments.length > 0 ? (
            <OriginalDocumentPreview archiveId={row?.id || id} files={voucherData.attachments} />
          ) : (
            <div className="flex items-center justify-center h-full text-slate-400">
              <p>暂无附件</p>
            </div>
          )}
        </div>
      ),
    },
    {
      key: 'workflow',
      label: '协作与版本',
      children: (
        <div style={{ minHeight: 'calc(100vh - 250px)' }}>
          <DocumentWorkflowPanel archiveId={id} archiveTitle={archiveData?.title || row?.code || id} />
        </div>
      ),
    },
  ];

  return (
    <div className="min-h-screen bg-slate-50" data-archive-id={id}>
      {/* Header */}
      <div className="bg-white border-b border-slate-200 sticky top-0 z-10">
        <div className="max-w-[1600px] mx-auto px-6 py-4">
          <Breadcrumb
            items={[
              { title: '档案管理' },
              { title: '凭证详情' },
              { title: row?.code || id }
            ]}
            className="mb-4"
          />
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={handleBack}
                className="p-2 hover:bg-slate-100 rounded-lg text-slate-600 transition-colors"
                title="返回"
              >
                <ArrowLeft size={20} />
              </button>
              <div className="flex items-center gap-3">
                <div className="p-2 bg-primary-100 text-primary-600 rounded-lg">
                  <FileText size={20} />
                </div>
                <div>
                  <h1 className="text-2xl font-bold text-slate-800">凭证详情</h1>
                  <p className="text-sm text-slate-500 font-mono">{row?.code || id}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="max-w-[1600px] mx-auto px-6 py-6">
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <Tabs
            activeKey={activeTab}
            onChange={(key) => setActiveTab(key as TabKey)}
            className="w-full"
            items={tabItems}
          />
        </div>
      </div>
    </div>
  );
};

export default ArchiveDetailPage;
