/**
 * Watermark Components Module Manifest
 *
 * Module: Watermark overlay for security
 * Owner: platform-team
 */
import type { ModuleManifest } from '../../types/manifest';

export const moduleManifest: ModuleManifest = {
  id: 'component.watermark',
  owner: 'platform-team',
  publicApi: './index.ts',

  usedBy: [
    'src/**/*'
  ],

  canImportFrom: [
    'src/utils/**'
  ],

  restrictions: {
    disallowDeepImport: true,
    allowSharedDependencies: true
  },

  tags: ['component', 'shared', 'security']
};
