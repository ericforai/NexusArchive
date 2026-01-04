/**
 * Utils Module Manifest
 *
 * Module: Shared utility functions
 * Owner: platform-team
 */
import type { ModuleManifest } from '../types/manifest';

export const moduleManifest: ModuleManifest = {
  id: 'shared.utils',
  owner: 'platform-team',
  publicApi: './index.ts',

  usedBy: [
    'src/**/*'
  ],

  canImportFrom: [
    // Utils should only import from other utils or external libs
    'src/utils/**'
  ],

  restrictions: {
    disallowDeepImport: true,
    allowSharedDependencies: true
  },

  tags: ['shared', 'utils', 'stable', 'foundational']
};
