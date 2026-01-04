/**
 * Operations Pages Module Manifest
 *
 * Module: Operations management pages
 * Owner: platform-team
 */
import type { ModuleManifest } from '../../types/manifest';

export const moduleManifest: ModuleManifest = {
  id: 'page.operations',
  owner: 'platform-team',
  publicApi: './index.ts',

  canImportFrom: [
    'src/utils/**',
    'src/api/**',
    'src/components/**',
    'src/features/**/*'
  ],

  restrictions: {
    disallowDeepImport: true,
    allowSharedDependencies: true
  },

  tags: ['page', 'operations']
};
