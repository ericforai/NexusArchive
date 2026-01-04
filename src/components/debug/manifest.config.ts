/**
 * Debug Components Module Manifest
 *
 * Module: Development and debugging components
 * Owner: platform-team
 */
import type { ModuleManifest } from '../../types/manifest';

export const moduleManifest: ModuleManifest = {
  id: 'component.debug',
  owner: 'platform-team',
  publicApi: './index.ts',

  usedBy: [], // Only used in development

  canImportFrom: [
    'src/utils/**',
    'src/api/**',
    'src/components/common/**'
  ],

  restrictions: {
    disallowDeepImport: true,
    allowSharedDependencies: true
  },

  tags: ['component', 'development-only', 'debug']
};
