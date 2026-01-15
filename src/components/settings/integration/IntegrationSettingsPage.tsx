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
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Settings } from 'lucide-react';
import { useErpConfigManager } from './hooks/useErpConfigManager';
import { useScenarioSyncManager } from './hooks/useScenarioSyncManager';
import { useConnectorModal } from './hooks/useConnectorModal';
import { useIntegrationDiagnosis } from './hooks/useIntegrationDiagnosis';
import { useParamsEditor } from './hooks/useParamsEditor';
import { ErpConfigList } from './components/ErpConfigList';
import { ConnectorForm } from './components/ConnectorForm';
import { DiagnosisPanel } from './components/DiagnosisPanel';
import { ParamsEditor } from './components/ParamsEditor';
import { ScenarioDrawer } from './components/ScenarioDrawer';
import { ScenarioStatus } from '../../../types';

interface IntegrationSettingsPageProps {
  erpApi: any;
}

export function IntegrationSettingsPage({ erpApi }: IntegrationSettingsPageProps) {
  // Hook: ERP Config Manager
  const configManager = useErpConfigManager({ erpApi });

  // Hook: Scenario Sync Manager
  const scenarioManager = useScenarioSyncManager({ erpApi });

  // Drawer state
  const [drawerConfigId, setDrawerConfigId] = useState<number | null>(null);

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

  // DO NOT auto-load scenarios anymore - they will be loaded on demand via expand/collapse

  // Calculate scenario counts per config
  const scenarioCounts = useMemo(() => {
    const counts: Record<number, number> = {};
    scenarioManager.state.scenarios.forEach(s => {
      counts[s.configId] = (counts[s.configId] || 0) + 1;
    });
    return counts;
  }, [scenarioManager.state.scenarios]);

  // Calculate scenario statistics (running and error counts)
  const scenarioStats = useMemo(() => {
    const stats: Record<number, { running: number; error: number }> = {};
    scenarioManager.state.scenarios.forEach(s => {
      if (!stats[s.configId]) {
        stats[s.configId] = { running: 0, error: 0 };
      }
      // Note: Use lastSyncStatus to determine scenario status
      const status = s.lastSyncStatus === 'RUNNING' ? 'running' :
        s.lastSyncStatus === 'FAIL' ? 'error' : 'idle';
      if (status === 'running') stats[s.configId].running++;
      if (status === 'error') stats[s.configId].error++;
    });
    return stats;
  }, [scenarioManager.state.scenarios]);

  // Get current drawer config
  const drawerConfig = useMemo(() => {
    if (drawerConfigId === null) return null;
    return configManager.state.configs.find(c => c.id === drawerConfigId);
  }, [drawerConfigId, configManager.state.configs]);

  // Get current drawer scenarios
  const drawerScenarios = useMemo(() => {
    if (drawerConfigId === null) return [];
    return scenarioManager.state.scenarios
      .filter(s => s.configId === drawerConfigId)
      .map(s => {
        const status: ScenarioStatus = s.lastSyncStatus === 'RUNNING' ? 'running' :
          s.lastSyncStatus === 'FAIL' ? 'error' :
            s.lastSyncStatus === 'SUCCESS' ? 'success' : 'idle';
        return {
          id: s.id,
          name: s.name,
          status,
          lastSyncTime: s.lastSyncTime
        };
      });
  }, [drawerConfigId, scenarioManager.state.scenarios]);

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
        scenarioCounts={scenarioCounts}
        runningCounts={scenarioStats}
        onConfig={(config) => connectorModal.actions.openModal(config)}
        onTest={configManager.actions.testConnection}
        onDiagnose={diagnosis.actions.startDiagnosis}
        onReconcile={(_id) => {
          // TODO: implement reconcile
          console.log('Reconcile not implemented yet');
        }}
        onViewDetails={(configId) => {
          // Load scenarios if not already loaded
          scenarioManager.actions.loadScenarios(configId);
          setDrawerConfigId(configId);
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

      {/* Scenario Drawer */}
      {drawerConfig && (
        <ScenarioDrawer
          visible={drawerConfigId !== null}
          configName={drawerConfig.name}
          scenarios={drawerScenarios}
          onClose={() => setDrawerConfigId(null)}
          onSync={(scenarioId) => {
            scenarioManager.actions.syncScenario(scenarioId);
          }}
        />
      )}
    </div>
  );
}
