// Input: All hooks, components, types
// Output: IntegrationSettingsPage compositor component
// Pos: src/components/settings/integration//
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * Integration Settings Page - Refactored
 *
 * Compositor pattern: combines specialized hooks and components
 * Original: 1,709 lines → Refactored: ~150 lines
 */
import React, { useCallback, useEffect } from 'react';
import { Settings } from 'lucide-react';
import { Modal } from 'antd';
import { useErpConfigManager } from './hooks/useErpConfigManager';
import { useScenarioSyncManager } from './hooks/useScenarioSyncManager';
import { useConnectorModal } from './hooks/useConnectorModal';
import { useIntegrationDiagnosis } from './hooks/useIntegrationDiagnosis';
import { useParamsEditor } from './hooks/useParamsEditor';
import { ErpConfigList } from './components/ErpConfigList';
import { ConnectorForm } from './components/ConnectorForm';
import { DiagnosisPanel } from './components/DiagnosisPanel';
import { ParamsEditor } from './components/ParamsEditor';

interface IntegrationSettingsPageProps {
  erpApi: any;
}

export function IntegrationSettingsPage({ erpApi }: IntegrationSettingsPageProps) {
  // Hook: ERP Config Manager
  const configManager = useErpConfigManager({ erpApi });

  // Hook: Scenario Sync Manager
  const scenarioManager = useScenarioSyncManager({ erpApi });

  // Hook: Params Editor
  const paramsEditor = useParamsEditor({
    erpApi,
    onSyncComplete: useCallback(() => {
      if (scenarioManager.state.activeScenarioId) {
        scenarioManager.actions.loadSyncHistory(scenarioManager.state.activeScenarioId);
      }
    }, [scenarioManager.actions, scenarioManager.state.activeScenarioId]),
  });

  // Hook: Connector Modal
  const connectorModal = useConnectorModal({
    erpApi,
    onConfigSaved: configManager.actions.loadConfigs,
  });

  // Hook: Diagnosis
  const diagnosis = useIntegrationDiagnosis({ erpApi });

  // Initial load
  useEffect(() => {
    configManager.actions.loadConfigs();
  }, [configManager.actions]);

  // Load scenarios when active config changes
  useEffect(() => {
    if (configManager.state.activeConfigId) {
      scenarioManager.actions.loadScenarios(configManager.state.activeConfigId);
    }
  }, [configManager.state.activeConfigId, scenarioManager.actions]);

  const handleDeleteConfig = useCallback(async (configId: number) => {
    const config = configManager.state.configs.find(c => c.id === configId);
    if (!config) return;

    Modal.confirm({
      title: '确认移除连接器',
      content: (
        <div>
          <p>移除后,该连接器的所有同步记录将被保留,但不会再同步新数据。</p>
          <p className="font-semibold text-red-600 mt-2">
            此操作不可撤销,是否继续?
          </p>
        </div>
      ),
      okText: '确认移除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        await configManager.actions.deleteConfig(configId);
      },
    });
  }, [configManager.actions, configManager.state.configs]);

  return (
    <div className="integration-settings p-6">
      {/* Page Header */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <Settings size={24} />
          集成设置
        </h1>
        <button
          onClick={() => connectorModal.actions.openModal()}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          + 添加连接器
        </button>
      </div>

      {/* Connector Grid */}
      <ErpConfigList
        configs={configManager.state.configs}
        onConfig={(config) => connectorModal.actions.openModal(config)}
        onDelete={handleDeleteConfig}
        onTest={configManager.actions.testConnection}
        onDiagnose={diagnosis.actions.startDiagnosis}
        onReconcile={(id) => {
          // TODO: implement reconcile
          console.log('Reconcile not implemented yet');
        }}
      />

      {/* Modals */}
      {connectorModal.state.show && (
        <ConnectorForm
          state={connectorModal.state}
          actions={connectorModal.actions}
        />
      )}

      {diagnosis.state.show && (
        <DiagnosisPanel
          state={diagnosis.state}
          actions={diagnosis.actions}
        />
      )}

      {paramsEditor.state.showFor && (
        <ParamsEditor
          state={paramsEditor.state}
          actions={paramsEditor.actions}
        />
      )}
    </div>
  );
}
