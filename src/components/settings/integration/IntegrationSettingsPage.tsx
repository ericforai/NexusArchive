/**
 * Integration Settings Page - Refactored
 *
 * Compositor pattern: combines specialized hooks and components
 * Original: 1,709 lines → Refactored: ~150 lines
 */
import React, { useEffect, useCallback, useRef } from 'react';
import { Settings, RefreshCw } from 'lucide-react';
import { ErpConfig } from '../../../types';
import { useErpConfigManager } from './hooks/useErpConfigManager';
import { useScenarioSyncManager } from './hooks/useScenarioSyncManager';
import { useConnectorModal } from './hooks/useConnectorModal';
import { useIntegrationDiagnosis } from './hooks/useIntegrationDiagnosis';
import { useParamsEditor } from './hooks/useParamsEditor';
import { useAiAdapterHandler } from './hooks/useAiAdapterHandler';
import { ErpConfigList } from './components/ErpConfigList';
import { ScenarioCard } from './components/ScenarioCard';
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

  // Use ref to track pending sync scenario ID
  const pendingSyncScenarioIdRef = useRef<number | null>(null);

  // Hook: Params Editor with sync completion callback
  const paramsEditor = useParamsEditor({
    erpApi,
    onSyncComplete: useCallback(() => {
      // Reload sync history for the scenario that was just synced
      if (pendingSyncScenarioIdRef.current) {
        scenarioManager.actions.loadSyncHistory(pendingSyncScenarioIdRef.current);
      }
    }, [scenarioManager.actions]),
  });

  // Hook: Connector Modal
  const connectorModal = useConnectorModal({
    erpApi,
    onConfigSaved: configManager.actions.loadConfigs,
  });

  // Hook: Diagnosis
  const diagnosis = useIntegrationDiagnosis({ erpApi });

  // Hook: AI Adapter
  const aiAdapter = useAiAdapterHandler({ erpApi });

  // Update ref when params editor opens
  useEffect(() => {
    if (paramsEditor.state.showFor) {
      pendingSyncScenarioIdRef.current = paramsEditor.state.showFor;
    }
  }, [paramsEditor.state.showFor]);

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

  const handleSelectConfig = (config: ErpConfig) => {
    configManager.actions.setActiveConfig(config.id);
  };

  return (
    <div className="integration-settings p-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <Settings size={24} />
          集成设置
        </h1>
        <button
          onClick={() => configManager.actions.loadConfigs()}
          className="flex items-center gap-2 px-4 py-2 border rounded hover:bg-gray-50"
        >
          <RefreshCw size={16} />
          刷新
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left: ERP Config List */}
        <div className="lg:col-span-1">
          <div className="border rounded-lg p-4">
            <h2 className="font-semibold mb-4">ERP 连接器</h2>
            <ErpConfigList
              state={configManager.state}
              actions={configManager.actions}
              onSelectConfig={handleSelectConfig}
            />
            <button
              onClick={() => connectorModal.actions.openModal()}
              className="w-full mt-4 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
            >
              添加连接器
            </button>
          </div>
        </div>

        {/* Right: Scenarios */}
        <div className="lg:col-span-2">
          {configManager.state.activeConfigId && (
            <div className="space-y-4">
              {scenarioManager.state.scenarios.map(scenario => (
                <ScenarioCard
                  key={scenario.id}
                  scenario={scenario}
                  state={scenarioManager.state}
                  actions={scenarioManager.actions}
                  onOpenParams={paramsEditor.actions.openEditor}
                  onViewHistory={(id) => scenarioManager.actions.toggleHistoryView(id)}
                />
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Connector Modal */}
      {connectorModal.state.show && (
        <ConnectorForm
          state={connectorModal.state}
          actions={connectorModal.actions}
        />
      )}

      {/* Diagnosis Panel */}
      {diagnosis.state.show && (
        <DiagnosisPanel
          state={diagnosis.state}
          actions={diagnosis.actions}
        />
      )}

      {/* Params Editor Modal */}
      {paramsEditor.state.showFor && (
        <ParamsEditor
          state={paramsEditor.state}
          actions={paramsEditor.actions}
        />
      )}
    </div>
  );
}
