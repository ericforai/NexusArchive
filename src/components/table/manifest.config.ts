/**
 * Table Components Module Manifest
 *
 * Module: Advanced table component with preview actions
 * Owner: platform-team
 */
import type { ModuleManifest } from '../../types/manifest';

export const moduleManifest: ModuleManifest = {
  id: 'component.table',
  owner: 'platform-team',
  publicApi: './index.ts',

  usedBy: [
    'src/features/**/*',
    'src/pages/**/*'
  ],

  canImportFrom: [
    'src/utils/**',
    'src/api/**',
    'src/components/common/**'
  ],

  restrictions: {
    disallowDeepImport: true,
    allowSharedDependencies: true
  },

  tags: ['component', 'shared', 'stable', 'complex']
};
