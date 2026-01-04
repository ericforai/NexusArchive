/**
 * Archives Feature Module Manifest
 *
 * Module: Archive management and viewing
 * Owner: platform-team
 */
import type { ModuleManifest } from '../../types/manifest';

export const moduleManifest: ModuleManifest = {
  id: 'feature.archives',
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

  tags: ['feature', 'core', 'stable']
};
