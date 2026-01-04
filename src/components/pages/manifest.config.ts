/**
 * Shared Pages Components Module Manifest
 *
 * Module: Shared/reusable page components that can be used across different page directories
 * Owner: platform-team
 */
import type { ModuleManifest } from '../../types/manifest';

export const moduleManifest: ModuleManifest = {
  id: 'component.pages',
  owner: 'platform-team',
  publicApi: './index.ts',

  usedBy: [
    'src/pages/**/*'
  ],

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

  tags: ['component', 'shared', 'pages']
};
