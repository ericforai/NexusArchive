/**
 * Common Components Module Manifest
 *
 * Module: Shared UI components used across the application
 * Owner: platform-team
 */
import type { ModuleManifest } from '../../types/manifest';

export const moduleManifest: ModuleManifest = {
  id: 'component.common',
  owner: 'platform-team',
  publicApi: './index.ts',

  usedBy: [
    'src/features/**/*',
    'src/pages/**/*',
    'src/components/**/*'
  ],

  canImportFrom: [
    'src/utils/**',
    'src/api/**'
  ],

  restrictions: {
    disallowDeepImport: true,
    allowSharedDependencies: true
  },

  tags: ['component', 'shared', 'stable']
};
