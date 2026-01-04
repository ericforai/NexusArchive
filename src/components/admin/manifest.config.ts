/**
 * Admin Components Module Manifest
 *
 * Module: Administrative interface components
 * Owner: platform-team
 */
import type { ModuleManifest } from '../../types/manifest';

export const moduleManifest: ModuleManifest = {
  id: 'component.admin',
  owner: 'platform-team',
  publicApi: './index.ts',

  usedBy: [
    'src/pages/admin/**/*'
  ],

  canImportFrom: [
    'src/utils/**',
    'src/api/**',
    'src/components/common/**',
    'src/components/org/**'
  ],

  restrictions: {
    disallowDeepImport: true,
    allowSharedDependencies: true
  },

  tags: ['component', 'domain-specific']
};
