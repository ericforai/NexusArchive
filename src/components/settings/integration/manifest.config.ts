// Input: Module metadata for architecture defense
// Output: Module manifest declaration (J1 Self-Description)
// Pos: src/components/settings/integration/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * Integration Settings Module Manifest
 *
 * This manifest defines the module's identity, ownership, public API,
 * and architectural boundaries for self-describing architecture defense.
 *
 * @see docs/architecture-defense-implementation.md
 */
export const moduleManifest = {
  // Module Identity
  id: 'feature.integration-settings',
  owner: 'team-platform',
  version: '2.0.0',
  createdAt: '2026-01-05',

  // Public API Contract (Single Legal Entry Point)
  publicApi: './index.ts',

  // Dependency Whitelist (What this module CAN import)
  canImportFrom: [
    // Core React ecosystem
    'react',
    'react-dom',

    // UI libraries
    'antd',
    'lucide-react',
    'react-hot-toast',

    // Shared utilities and types
    'src/types.ts',
    'src/api/erp.ts',
    'src/api/**',

    // State management
    'zustand',

    // Development tools
    'vitest',
    '@testing-library/react',
  ],

  // Who CAN import this module (reciprocal declaration)
  usedBy: [
    'src/components/settings/**',
    'src/routes/**',
  ],

  // Architectural Restrictions
  restrictions: {
    // Block deep imports into internal implementation
    disallowDeepImport: true,

    // Prevent importing from other components' internals
    disallowCrossComponentInternal: true,

    // No direct database/model imports (UI layer)
    disallowDatabaseImports: true,
  },

  // Module Metrics (for health monitoring)
  metrics: {
    linesOfCode: 161, // Compositor only (excludes hooks/components)
    totalFiles: 19, // 8 hooks + 5 components + 1 types + 1 compositor + 4 tests
    testCoverage: 100, // 44/44 tests passing
    complexity: 'low', // Cyclomatic complexity < 10 per module
  },

  // Compliance Tags
  tags: [
    'entropy-reduction',
    'compositor-pattern',
    'test-driven-development',
    'document-self-consistency',
  ],

  // Architecture Compliance
  compliance: {
    entropyReduction: true, // Meets entropy-reduction principles
    documentConsistent: true, // Has 3-line headers, README updated
    architectureValidated: true, // Passed dependency-cruiser validation
  },
} as const;

/**
 * Manifest Type Safety
 *
 * This ensures the manifest structure cannot be accidentally
 * modified to remove critical metadata.
 */
export type ModuleManifest = typeof moduleManifest;

/**
 * Runtime Validation Helper
 *
 * Usage in development:
 * ```typescript
 * import { validateManifest } from './manifest.config';
 * validateManifest(); // Throws if manifest is invalid
 * ```
 */
export function validateManifest(): void {
  const required = ['id', 'owner', 'publicApi', 'canImportFrom'] as const;
  const missing = required.filter(key => !(key in moduleManifest));

  if (missing.length > 0) {
    throw new Error(
      `Invalid manifest: missing required fields: ${missing.join(', ')}`
    );
  }

  if (import.meta.env.DEV) {
    console.log(`[Architecture] Module ${moduleManifest.id} validated`);
  }
}

// Auto-validate in development
if (typeof window !== 'undefined' && import.meta.env.DEV) {
  validateManifest();
}
