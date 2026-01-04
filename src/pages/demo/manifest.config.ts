/**
 * Demo Pages Module Manifest
 *
 * Module: Demo/example pages
 * Owner: platform-team
 */
import type { ModuleManifest } from '../../types/manifest';

export const moduleManifest: ModuleManifest = {
  id: 'page.demo',
  owner: 'platform-team',
  publicApi: './index.ts',

  canImportFrom: [
    'src/utils/**',
    'src/api/**',
    'src/components/**'
  ],

  restrictions: {
    disallowDeepImport: true,
    allowSharedDependencies: true
  },

  tags: ['page', 'development-only', 'demo']
};
