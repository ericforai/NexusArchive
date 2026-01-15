/**
 * Architecture Introspection System
 *
 * Implements J4: Reflex - Runtime architecture visibility
 *
 * Provides runtime access to module manifests for debugging and validation
 */

import type { ModuleManifest, RuntimeModuleInfo, ValidationResult } from '../types/manifest';

/**
 * Architecture registry for runtime introspection
 */
class ArchitectureRegistry {
  private modules: Map<string, RuntimeModuleInfo> = new Map();
  public initialized = false;

  /**
   * Register a module manifest
   */
  register(manifest: ModuleManifest, category: string, path: string): void {
    const info: RuntimeModuleInfo = {
      ...manifest,
      category: category as any,
      path,
      hasPublicApi: true, // Will be validated at build time
      dependencyCount: 0  // Will be calculated at build time
    };

    this.modules.set(manifest.id, info);
  }

  /**
   * Get module by ID
   */
  getModule(id: string): RuntimeModuleInfo | undefined {
    return this.modules.get(id);
  }

  /**
   * Get owner for a module
   */
  getOwner(id: string): string | undefined {
    return this.modules.get(id)?.owner;
  }

  /**
   * Get all modules
   */
  getAllModules(): RuntimeModuleInfo[] {
    return Array.from(this.modules.values());
  }

  /**
   * Get modules by tag
   */
  getModulesByTag(tag: string): RuntimeModuleInfo[] {
    return this.getAllModules().filter(m => m.tags?.includes(tag));
  }

  /**
   * Get modules by owner
   */
  getModulesByOwner(owner: string): RuntimeModuleInfo[] {
    return this.getAllModules().filter(m => m.owner === owner);
  }

  /**
   * Validate module dependencies (basic check)
   */
  validate(): ValidationResult {
    const errors: ValidationResult['errors'] = [];

    for (const [id, module] of this.modules) {
      // Check if module has owner
      if (!module.owner) {
        errors.push({
          module: id,
          issue: 'No owner specified',
          severity: 'error'
        });
      }

      // Check if module has public API
      if (!module.publicApi) {
        errors.push({
          module: id,
          issue: 'No public API specified',
          severity: 'error'
        });
      }

      // Check for legacy modules without compliance target
      if (module.tags?.includes('legacy') && !module.complianceTarget) {
        errors.push({
          module: id,
          issue: 'Legacy module missing compliance target',
          severity: 'warn'
        });
      }

      // Check for exceptions without review date
      if (module.exception && !module.reviewDate) {
        errors.push({
          module: id,
          issue: 'Exception module missing review date',
          severity: 'error'
        });
      }
    }

    return {
      valid: errors.filter(e => e.severity === 'error').length === 0,
      errors
    };
  }

  /**
   * Export to window.__ARCH__ for browser console access
   */
  exposeToGlobal(): void {
    if (typeof window !== 'undefined') {
      window.__ARCH__ = {
        modules: this.getAllModules(),
        getModule: (id: string) => this.getModule(id),
        getOwner: (id: string) => this.getOwner(id),
        validate: () => this.validate()
      };
    }
  }
}

// Singleton instance
const registry = new ArchitectureRegistry();

/**
 * Initialize architecture introspection with module manifests
 */
export function initializeArchitectureIntrospection(): void {
  if (registry.initialized) return;

  // Register feature modules
  try {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const { moduleManifest: archivesManifest } = require('../features/archives/manifest.config');
    registry.register(archivesManifest, 'feature', 'src/features/archives');
  } catch (_e) {
    // Module not found or no manifest
  }

  try {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const { moduleManifest: settingsManifest } = require('../features/settings/manifest.config');
    registry.register(settingsManifest, 'feature', 'src/features/settings');
  } catch (_e) {
    // Module not found or no manifest
  }

  try {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const { moduleManifest: borrowingManifest } = require('../features/borrowing/manifest.config');
    registry.register(borrowingManifest, 'feature', 'src/features/borrowing');
  } catch (_e) {
    // Module not found or no manifest
  }

  try {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const { moduleManifest: complianceManifest } = require('../features/compliance/manifest.config');
    registry.register(complianceManifest, 'feature', 'src/features/compliance');
  } catch (_e) {
    // Module not found or no manifest
  }

  // Register component modules
  try {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const { moduleManifest: commonManifest } = require('../components/common/manifest.config');
    registry.register(commonManifest, 'component', 'src/components/common');
  } catch (_e) {
    // Module not found or no manifest
  }

  try {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const { moduleManifest: tableManifest } = require('../components/table/manifest.config');
    registry.register(tableManifest, 'component', 'src/components/table');
  } catch (_e) {
    // Module not found or no manifest
  }

  try {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const { moduleManifest: layoutManifest } = require('../components/layout/manifest.config');
    registry.register(layoutManifest, 'component', 'src/components/layout');
  } catch (_e) {
    // Module not found or no manifest
  }

  // Register utils
  try {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const { moduleManifest: utilsManifest } = require('../utils/manifest.config');
    registry.register(utilsManifest, 'util', 'src/utils');
  } catch (_e) {
    // Module not found or no manifest
  }

  // Expose to global
  registry.exposeToGlobal();
  registry.initialized = true;
}

/**
 * Get the registry instance
 */
export function getRegistry(): ArchitectureRegistry {
  return registry;
}

// Auto-initialize in development
if (import.meta.env.DEV) {
  initializeArchitectureIntrospection();
}
