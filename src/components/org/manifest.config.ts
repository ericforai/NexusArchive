/**
 * Organization Components Module Manifest
 *
 * Module: Organization/enterprise related components
 * Owner: platform-team
 */
import type { ModuleManifest } from '../../types/manifest';

export const moduleManifest: ModuleManifest = {
  id: 'component.org',
  owner: 'platform-team',
  publicApi: './index.ts',

  usedBy: [
    'src/features/**/*',
    'src/pages/admin/**/*'
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
