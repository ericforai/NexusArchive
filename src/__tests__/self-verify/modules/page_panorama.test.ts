// Input: Module manifest contract
// Output: Minimal self-verify guard for module page.panorama
// Pos: Self-verifying tests - module baseline
// @manifest:page.panorama

import { describe, expect, it } from 'vitest';
import { moduleManifest } from '../../../pages/panorama/manifest.config';

describe('self-verify: page.panorama', () => {
  it('manifest contract should be stable', () => {
    expect(moduleManifest.id).toBe('page.panorama');
    expect(typeof moduleManifest.owner).toBe('string');
    expect(moduleManifest.owner.length).toBeGreaterThan(0);
    expect(typeof moduleManifest.publicApi).toBe('string');
    expect(moduleManifest.publicApi.length).toBeGreaterThan(0);
    expect(Array.isArray(moduleManifest.tags)).toBe(true);
    expect(moduleManifest.tags?.length ?? 0).toBeGreaterThan(0);
  });
});
