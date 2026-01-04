/**
 * Security Pages Module Manifest
 *
 * Module: Security related pages
 * Owner: platform-team
 */
import type { ModuleManifest } from '../../types/manifest';

export const moduleManifest: ModuleManifest = {
  id: 'page.security',
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

  tags: ['page', 'security']
};
