/**
 * Layout Components Module Manifest
 *
 * Module: Application layout components (header, sidebar, etc.)
 * Owner: platform-team
 */
import type { ModuleManifest } from '../../types/manifest';

export const moduleManifest: ModuleManifest = {
  id: 'component.layout',
  owner: 'platform-team',
  publicApi: './index.ts',

  usedBy: [
    'src/pages/**/*'
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

  tags: ['component', 'shared', 'stable']
};
