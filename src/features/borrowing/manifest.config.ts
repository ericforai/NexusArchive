/**
 * Borrowing Feature Module Manifest
 *
 * Module: Archive borrowing/lending workflow
 * Owner: platform-team
 */
import type { ModuleManifest } from '../../types/manifest';

export const moduleManifest: ModuleManifest = {
  id: 'feature.borrowing',
  owner: 'platform-team',
  publicApi: './index.ts',

  canImportFrom: [
    'src/shared/utils/**',
    'src/api/**',
    'src/components/**',
    'src/features/*/index.ts',
    'src/utils/**'
  ],

  restrictions: {
    disallowDeepImport: true,
    allowSharedDependencies: true
  },

  tags: ['feature', 'core']
};
