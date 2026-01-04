/**
 * Module Manifest System
 *
 * Implements J1: Self-Description - Each module declares its boundaries
 *
 * Part of Architecture Defense System
 */

export interface ModuleManifest {
  /** Unique module identifier (e.g., "feature.archives", "component.table") */
  id: string;

  /** Team/person responsible for this module */
  owner: string;

  /** Public API entry point - single legal import path for consumers */
  publicApi: string;

  /** What this module is allowed to import (whitelist) */
  canImportFrom?: string[];

  /** For shared modules: who is allowed to use this module */
  usedBy?: string[];

  /** Module restrictions */
  restrictions?: {
    /** Block deep imports (e.g., from ../../feature/internal/) */
    disallowDeepImport?: boolean;
    /** Allow dependencies on other shared modules */
    allowSharedDependencies?: boolean;
  };

  /** Module tags for categorization */
  tags?: string[];

  /** Target date for compliance (for legacy modules) */
  complianceTarget?: string;

  /** Temporary exemption flag */
  exception?: boolean;

  /** Reason for exemption */
  exceptionReason?: string;

  /** Date when exception must be reviewed */
  reviewDate?: string;
}

/**
 * Module category for runtime introspection
 */
export type ModuleCategory =
  | 'feature'
  | 'component'
  | 'shared'
  | 'page'
  | 'layout'
  | 'hook'
  | 'util';

/**
 * Runtime module information
 */
export interface RuntimeModuleInfo extends ModuleManifest {
  category: ModuleCategory;
  path: string;
  hasPublicApi: boolean;
  dependencyCount: number;
}

/**
 * Global runtime architecture registry
 */
declare global {
  interface Window {
    __ARCH__?: {
      modules: RuntimeModuleInfo[];
      getModule: (id: string) => RuntimeModuleInfo | undefined;
      getOwner: (id: string) => string | undefined;
      validate: () => ValidationResult;
    };
  }
}

export interface ValidationResult {
  valid: boolean;
  errors: Array<{
    module: string;
    issue: string;
    severity: 'error' | 'warn';
  }>;
}
