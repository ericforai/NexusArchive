/**
 * Settings Feature Module Manifest
 *
 * Module: User and system settings
 * Owner: platform-team
 */
import type { ModuleManifest } from '../../types/manifest';

export const moduleManifest: ModuleManifest = {
  id: 'feature.settings',
  owner: 'platform-team',
  publicApi: './index.ts',

  canImportFrom: [
    'src/shared/utils/**',
    'src/api/**',
    'src/components/**',
    'src/features/*/index.ts',
    'src/utils/**'
  ],

  restrictions: {
    disallowDeepImport: true,
    allowSharedDependencies: true
  },

  tags: ['feature', 'stable']
};
