/**
 * Pre-Archive Pages Module Manifest
 *
 * Module: Pre-archive processing pages
 * Owner: platform-team
 */
import type { ModuleManifest } from '../../types/manifest';

export const moduleManifest: ModuleManifest & {
  exports: Record<string, {
    role: string;
    file: string;
    capabilities: string[];
  }>;
} = {
  id: 'page.pre-archive',
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

  tags: ['page', 'archive'],

  exports: {
    // Pool components
    PoolPage: {
      role: 'page',
      file: 'PoolPage.tsx',
      capabilities: ['view-switch', 'pool-list', 'pool-kanban'],
    },
  },
};
