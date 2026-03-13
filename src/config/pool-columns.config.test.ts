// src/config/pool-columns.config.test.ts
import { describe, it, expect } from 'vitest';
import { POOL_COLUMN_GROUPS, getColumnByState, getSubStateLabel, getColumnStates, SimplifiedPreArchiveStatus, STATUS_CONFIG, DEFAULT_DASHBOARD_FILTER, LEGACY_STATUS_MAP, resolveStatus } from './pool-columns.config';

describe('PoolColumnsConfig', () => {
  describe('SimplifiedPreArchiveStatus', () => {
    it('should have 6 core states', () => {
      expect(Object.values(SimplifiedPreArchiveStatus)).toHaveLength(6);
    });

    it('should have PENDING_CHECK state', () => {
      expect(SimplifiedPreArchiveStatus.PENDING_CHECK).toBe('PENDING_CHECK');
    });

    it('should have NEEDS_ACTION state', () => {
      expect(SimplifiedPreArchiveStatus.NEEDS_ACTION).toBe('NEEDS_ACTION');
    });

    it('should have READY_TO_MATCH state', () => {
      expect(SimplifiedPreArchiveStatus.READY_TO_MATCH).toBe('READY_TO_MATCH');
    });

    it('should have READY_TO_ARCHIVE state', () => {
      expect(SimplifiedPreArchiveStatus.READY_TO_ARCHIVE).toBe('READY_TO_ARCHIVE');
    });

    it('should have SUBMITTED state', () => {
      expect(SimplifiedPreArchiveStatus.SUBMITTED).toBe('SUBMITTED');
    });

    it('should have COMPLETED state', () => {
      expect(SimplifiedPreArchiveStatus.COMPLETED).toBe('COMPLETED');
    });
  });

  describe('STATUS_CONFIG', () => {
    it('should have config for all 6 states', () => {
      expect(Object.keys(STATUS_CONFIG)).toHaveLength(6);
    });

    it('should have color, icon, label, and description for each state', () => {
      Object.values(STATUS_CONFIG).forEach(config => {
        expect(config).toHaveProperty('color');
        expect(config).toHaveProperty('icon');
        expect(config).toHaveProperty('label');
        expect(config).toHaveProperty('description');
        expect(typeof config.color).toBe('string');
        expect(typeof config.icon).toBe('string');
        expect(typeof config.label).toBe('string');
        expect(typeof config.description).toBe('string');
      });
    });

    it('should have correct config for READY_TO_ARCHIVE', () => {
      const config = STATUS_CONFIG[SimplifiedPreArchiveStatus.READY_TO_ARCHIVE];
      expect(config.color).toBe('#10b981');
      expect(config.icon).toBe('check-circle');
      expect(config.label).toBe('可归档');
      expect(config.description).toBe('已就绪，可以提交归档');
    });
  });

  describe('DEFAULT_DASHBOARD_FILTER', () => {
    it('should be set to READY_TO_ARCHIVE', () => {
      expect(DEFAULT_DASHBOARD_FILTER).toBe(SimplifiedPreArchiveStatus.READY_TO_ARCHIVE);
    });
  });

  describe('POOL_COLUMN_GROUPS', () => {
    it('should have 5 column groups', () => {
      expect(POOL_COLUMN_GROUPS).toHaveLength(5);
    });

    it('should have correct group structure', () => {
      const firstGroup = POOL_COLUMN_GROUPS[0];
      expect(firstGroup).toHaveProperty('id');
      expect(firstGroup).toHaveProperty('title');
      expect(firstGroup).toHaveProperty('subStates');
      expect(firstGroup).toHaveProperty('actions');
      // Only ready-to-archive group has highlight property
      const readyToArchiveGroup = POOL_COLUMN_GROUPS.find(g => g.id === 'ready-to-archive');
      expect(readyToArchiveGroup?.highlight).toBe(true);
    });

    it('should have correct subStates for pending group', () => {
      const pendingGroup = POOL_COLUMN_GROUPS.find(g => g.id === 'pending');
      expect(pendingGroup?.subStates).toEqual([
        { value: SimplifiedPreArchiveStatus.PENDING_CHECK, label: '待检测' },
      ]);
    });

    it('should have correct subStates for needs-action group', () => {
      const needsActionGroup = POOL_COLUMN_GROUPS.find(g => g.id === 'needs-action');
      expect(needsActionGroup?.subStates).toEqual([
        { value: SimplifiedPreArchiveStatus.NEEDS_ACTION, label: '待处理' },
      ]);
    });

    it('should have correct subStates for ready-to-match group', () => {
      const readyToMatchGroup = POOL_COLUMN_GROUPS.find(g => g.id === 'ready-to-match');
      expect(readyToMatchGroup?.subStates).toEqual([
        { value: SimplifiedPreArchiveStatus.READY_TO_MATCH, label: '可匹配' },
      ]);
    });

    it('should have correct subStates for ready-to-archive group', () => {
      const readyToArchiveGroup = POOL_COLUMN_GROUPS.find(g => g.id === 'ready-to-archive');
      expect(readyToArchiveGroup?.subStates).toEqual([
        { value: SimplifiedPreArchiveStatus.READY_TO_ARCHIVE, label: '可归档' },
      ]);
      expect(readyToArchiveGroup?.highlight).toBe(true);
    });

    it('should have correct subStates for completed group', () => {
      const completedGroup = POOL_COLUMN_GROUPS.find(g => g.id === 'completed');
      expect(completedGroup?.subStates).toEqual([
        { value: SimplifiedPreArchiveStatus.COMPLETED, label: '已完成' },
      ]);
    });

    it('should have all 5 subStates distributed across 5 groups', () => {
      const allSubStates = POOL_COLUMN_GROUPS.flatMap(g => g.subStates.map(s => s.value));
      expect(allSubStates).toHaveLength(5);
      expect(allSubStates).toContain(SimplifiedPreArchiveStatus.PENDING_CHECK);
      expect(allSubStates).toContain(SimplifiedPreArchiveStatus.NEEDS_ACTION);
      expect(allSubStates).toContain(SimplifiedPreArchiveStatus.READY_TO_MATCH);
      expect(allSubStates).toContain(SimplifiedPreArchiveStatus.READY_TO_ARCHIVE);
      expect(allSubStates).toContain(SimplifiedPreArchiveStatus.COMPLETED);
    });
  });

  describe('getColumnByState', () => {
    it('should return correct column for PENDING_CHECK state', () => {
      const result = getColumnByState(SimplifiedPreArchiveStatus.PENDING_CHECK);
      expect(result?.id).toBe('pending');
    });

    it('should return correct column for NEEDS_ACTION state', () => {
      const result = getColumnByState(SimplifiedPreArchiveStatus.NEEDS_ACTION);
      expect(result?.id).toBe('needs-action');
    });

    it('should return correct column for READY_TO_MATCH state', () => {
      const result = getColumnByState(SimplifiedPreArchiveStatus.READY_TO_MATCH);
      expect(result?.id).toBe('ready-to-match');
    });

    it('should return correct column for READY_TO_ARCHIVE state', () => {
      const result = getColumnByState(SimplifiedPreArchiveStatus.READY_TO_ARCHIVE);
      expect(result?.id).toBe('ready-to-archive');
      expect(result?.highlight).toBe(true);
    });

    it('should return correct column for COMPLETED state', () => {
      const result = getColumnByState(SimplifiedPreArchiveStatus.COMPLETED);
      expect(result?.id).toBe('completed');
    });

    it('should return null for unknown state', () => {
      const result = getColumnByState('UNKNOWN' as any);
      expect(result).toBeNull();
    });
  });

  describe('getSubStateLabel', () => {
    it('should return correct label for PENDING_CHECK', () => {
      expect(getSubStateLabel(SimplifiedPreArchiveStatus.PENDING_CHECK)).toBe('待检测');
    });

    it('should return correct label for NEEDS_ACTION', () => {
      expect(getSubStateLabel(SimplifiedPreArchiveStatus.NEEDS_ACTION)).toBe('待处理');
    });

    it('should return correct label for READY_TO_MATCH', () => {
      expect(getSubStateLabel(SimplifiedPreArchiveStatus.READY_TO_MATCH)).toBe('可匹配');
    });

    it('should return correct label for READY_TO_ARCHIVE', () => {
      expect(getSubStateLabel(SimplifiedPreArchiveStatus.READY_TO_ARCHIVE)).toBe('可归档');
    });

    it('should return correct label for COMPLETED', () => {
      expect(getSubStateLabel(SimplifiedPreArchiveStatus.COMPLETED)).toBe('已完成');
    });

    it('should return state name for unknown state', () => {
      expect(getSubStateLabel('UNKNOWN' as any)).toBe('UNKNOWN');
    });
  });

  describe('getColumnStates', () => {
    it('should return correct states for pending column', () => {
      const result = getColumnStates('pending');
      expect(result).toEqual([SimplifiedPreArchiveStatus.PENDING_CHECK]);
    });

    it('should return correct states for needs-action column', () => {
      const result = getColumnStates('needs-action');
      expect(result).toEqual([SimplifiedPreArchiveStatus.NEEDS_ACTION]);
    });

    it('should return correct states for ready-to-match column', () => {
      const result = getColumnStates('ready-to-match');
      expect(result).toEqual([SimplifiedPreArchiveStatus.READY_TO_MATCH]);
    });

    it('should return correct states for ready-to-archive column', () => {
      const result = getColumnStates('ready-to-archive');
      expect(result).toEqual([SimplifiedPreArchiveStatus.READY_TO_ARCHIVE]);
    });

    it('should return correct states for completed column', () => {
      const result = getColumnStates('completed');
      expect(result).toEqual([SimplifiedPreArchiveStatus.COMPLETED]);
    });

    it('should return empty array for unknown column', () => {
      const result = getColumnStates('unknown');
      expect(result).toEqual([]);
    });
  });

  describe('Legacy Status Support', () => {
    describe('LEGACY_STATUS_MAP', () => {
      it('should map PENDING_CHECK correctly', () => {
        expect(LEGACY_STATUS_MAP['PENDING_CHECK']).toBe(SimplifiedPreArchiveStatus.PENDING_CHECK);
      });

      it('should map CHECK_FAILED to NEEDS_ACTION', () => {
        expect(LEGACY_STATUS_MAP['CHECK_FAILED']).toBe(SimplifiedPreArchiveStatus.NEEDS_ACTION);
      });

      it('should map PENDING_ARCHIVE to READY_TO_ARCHIVE', () => {
        expect(LEGACY_STATUS_MAP['PENDING_ARCHIVE']).toBe(SimplifiedPreArchiveStatus.READY_TO_ARCHIVE);
      });

      it('should map ARCHIVED to COMPLETED', () => {
        expect(LEGACY_STATUS_MAP['ARCHIVED']).toBe(SimplifiedPreArchiveStatus.COMPLETED);
      });
    });

    describe('resolveStatus', () => {
      it('should preserve standard statuses', () => {
        expect(resolveStatus(SimplifiedPreArchiveStatus.READY_TO_ARCHIVE)).toBe(SimplifiedPreArchiveStatus.READY_TO_ARCHIVE);
      });

      it('should resolve legacy status string correct', () => {
        expect(resolveStatus('PENDING_ARCHIVE')).toBe(SimplifiedPreArchiveStatus.READY_TO_ARCHIVE);
      });

      it('should resolve unknown status to NEEDS_ACTION (safe fallback)', () => {
        expect(resolveStatus('SOME_WEIRD_STATUS_FROM_BACKEND')).toBe(SimplifiedPreArchiveStatus.NEEDS_ACTION);
      });
    });
  });
});
