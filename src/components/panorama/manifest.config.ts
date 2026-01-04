/**
 * Panorama Components Module Manifest
 *
 * Module: Panorama/3D view related components
 * Owner: platform-team
 */
import type { ModuleManifest } from '../../types/manifest';

export const moduleManifest: ModuleManifest = {
  id: 'component.panorama',
  owner: 'platform-team',
  publicApi: './index.ts',

  usedBy: [
    'src/features/**/*',
    'src/pages/panorama/**/*'
  ],

  canImportFrom: [
    'src/utils/**',
    'src/api/**',
    'src/components/common/**',
    'src/components/voucher/**'
  ],

  restrictions: {
    disallowDeepImport: true,
    allowSharedDependencies: true
  },

  tags: ['component', 'domain-specific', 'complex']
};
