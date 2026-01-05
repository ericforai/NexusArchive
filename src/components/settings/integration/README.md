# Integration Settings Module

Refactored from 1,709 lines to 161 lines using compositor pattern.

## Structure

- `IntegrationSettingsPage.tsx` - Main compositor (161 lines)
- `hooks/` - Business logic hooks (8 hooks)
- `components/` - UI components (5 components)
- `types.ts` - Type definitions

## Hooks

- `useErpConfigManager` - ERP configuration management
- `useScenarioSyncManager` - Scenario sync management
- `useConnectorModal` - Connector modal state
- `useIntegrationDiagnosis` - Diagnosis functionality
- `useParamsEditor` - Parameter editor
- `useAiAdapterHandler` - AI adapter handling
- `useMonitoring` - Integration monitoring
- `useReconciliation` - Reconciliation records

## Components

- `ErpConfigList` - Config list UI
- `ScenarioCard` - Scenario cards
- `ConnectorForm` - Connector form modal
- `DiagnosisPanel` - Diagnosis panel
- `ParamsEditor` - Params editor modal

## Metrics

- **Original**: 1,709 lines
- **Refactored**: 161 lines
- **Reduction**: 91.1%
- **Hooks**: 8
- **Components**: 5
- **Tests**: 44 passing
