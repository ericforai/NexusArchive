/**
 * Settings Components Module Manifest
 *
 * Module: Settings related components
 * Owner: platform-team
 */
import type { ModuleManifest } from '../../types/manifest';

export const moduleManifest: ModuleManifest = {
  id: 'component.settings',
  owner: 'platform-team',
  publicApi: './index.ts',

  usedBy: [
    'src/features/**/*',
    'src/pages/settings/**/*'
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

  tags: ['component', 'domain-specific']
};
