// src/config/pool-columns.config.test.ts
import { describe, it, expect } from 'vitest';
import { POOL_COLUMN_GROUPS, getColumnByState, getSubStateLabel, getColumnStates } from './pool-columns.config';

describe('PoolColumnsConfig', () => {
  describe('POOL_COLUMN_GROUPS', () => {
    it('should have 4 column groups', () => {
      expect(POOL_COLUMN_GROUPS).toHaveLength(4);
    });

    it('should have correct group structure', () => {
      const firstGroup = POOL_COLUMN_GROUPS[0];
      expect(firstGroup).toHaveProperty('id');
      expect(firstGroup).toHaveProperty('title');
      expect(firstGroup).toHaveProperty('subStates');
      expect(firstGroup).toHaveProperty('actions');
    });

    it('should have correct subStates for pending group', () => {
      const pendingGroup = POOL_COLUMN_GROUPS.find(g => g.id === 'pending');
      expect(pendingGroup?.subStates).toEqual([
        { value: 'DRAFT', label: '草稿' },
        { value: 'PENDING_CHECK', label: '待检测' },
      ]);
    });

    it('should have correct subStates for needs-attention group', () => {
      const needsAttentionGroup = POOL_COLUMN_GROUPS.find(g => g.id === 'needs-attention');
      expect(needsAttentionGroup?.subStates).toEqual([
        { value: 'CHECK_FAILED', label: '检测失败' },
        { value: 'PENDING_METADATA', label: '待补录' },
      ]);
    });

    it('should have correct subStates for ready group', () => {
      const readyGroup = POOL_COLUMN_GROUPS.find(g => g.id === 'ready');
      expect(readyGroup?.subStates).toEqual([
        { value: 'MATCH_PENDING', label: '待匹配' },
        { value: 'MATCHED', label: '已匹配' },
      ]);
    });

    it('should have correct subStates for processing group', () => {
      const processingGroup = POOL_COLUMN_GROUPS.find(g => g.id === 'processing');
      expect(processingGroup?.subStates).toEqual([
        { value: 'PENDING_ARCHIVE', label: '待归档' },
        { value: 'PENDING_APPROVAL', label: '审批中' },
        { value: 'ARCHIVING', label: '归档中' },
        { value: 'ARCHIVED', label: '已归档' },
      ]);
    });

    it('should have all 10 subStates distributed across 4 groups', () => {
      const allSubStates = POOL_COLUMN_GROUPS.flatMap(g => g.subStates.map(s => s.value));
      expect(allSubStates).toHaveLength(10);
      expect(allSubStates).toContain('DRAFT');
      expect(allSubStates).toContain('PENDING_CHECK');
      expect(allSubStates).toContain('CHECK_FAILED');
      expect(allSubStates).toContain('PENDING_METADATA');
      expect(allSubStates).toContain('MATCH_PENDING');
      expect(allSubStates).toContain('MATCHED');
      expect(allSubStates).toContain('PENDING_ARCHIVE');
      expect(allSubStates).toContain('PENDING_APPROVAL');
      expect(allSubStates).toContain('ARCHIVING');
      expect(allSubStates).toContain('ARCHIVED');
    });
  });

  describe('getColumnByState', () => {
    it('should return correct column for DRAFT state', () => {
      const result = getColumnByState('DRAFT');
      expect(result?.id).toBe('pending');
    });

    it('should return correct column for PENDING_CHECK state', () => {
      const result = getColumnByState('PENDING_CHECK');
      expect(result?.id).toBe('pending');
    });

    it('should return correct column for CHECK_FAILED state', () => {
      const result = getColumnByState('CHECK_FAILED');
      expect(result?.id).toBe('needs-attention');
    });

    it('should return correct column for PENDING_METADATA state', () => {
      const result = getColumnByState('PENDING_METADATA');
      expect(result?.id).toBe('needs-attention');
    });

    it('should return correct column for MATCH_PENDING state', () => {
      const result = getColumnByState('MATCH_PENDING');
      expect(result?.id).toBe('ready');
    });

    it('should return correct column for MATCHED state', () => {
      const result = getColumnByState('MATCHED');
      expect(result?.id).toBe('ready');
    });

    it('should return correct column for PENDING_ARCHIVE state', () => {
      const result = getColumnByState('PENDING_ARCHIVE');
      expect(result?.id).toBe('processing');
    });

    it('should return correct column for PENDING_APPROVAL state', () => {
      const result = getColumnByState('PENDING_APPROVAL');
      expect(result?.id).toBe('processing');
    });

    it('should return correct column for ARCHIVING state', () => {
      const result = getColumnByState('ARCHIVING');
      expect(result?.id).toBe('processing');
    });

    it('should return correct column for ARCHIVED state', () => {
      const result = getColumnByState('ARCHIVED');
      expect(result?.id).toBe('processing');
    });

    it('should return null for unknown state', () => {
      const result = getColumnByState('UNKNOWN' as any);
      expect(result).toBeNull();
    });
  });

  describe('getSubStateLabel', () => {
    it('should return correct label for DRAFT', () => {
      expect(getSubStateLabel('DRAFT')).toBe('草稿');
    });

    it('should return correct label for PENDING_CHECK', () => {
      expect(getSubStateLabel('PENDING_CHECK')).toBe('待检测');
    });

    it('should return correct label for CHECK_FAILED', () => {
      expect(getSubStateLabel('CHECK_FAILED')).toBe('检测失败');
    });

    it('should return correct label for PENDING_METADATA', () => {
      expect(getSubStateLabel('PENDING_METADATA')).toBe('待补录');
    });

    it('should return correct label for MATCH_PENDING', () => {
      expect(getSubStateLabel('MATCH_PENDING')).toBe('待匹配');
    });

    it('should return correct label for MATCHED', () => {
      expect(getSubStateLabel('MATCHED')).toBe('已匹配');
    });

    it('should return correct label for PENDING_ARCHIVE', () => {
      expect(getSubStateLabel('PENDING_ARCHIVE')).toBe('待归档');
    });

    it('should return correct label for PENDING_APPROVAL', () => {
      expect(getSubStateLabel('PENDING_APPROVAL')).toBe('审批中');
    });

    it('should return correct label for ARCHIVING', () => {
      expect(getSubStateLabel('ARCHIVING')).toBe('归档中');
    });

    it('should return correct label for ARCHIVED', () => {
      expect(getSubStateLabel('ARCHIVED')).toBe('已归档');
    });

    it('should return state name for unknown state', () => {
      expect(getSubStateLabel('UNKNOWN' as any)).toBe('UNKNOWN');
    });
  });

  describe('getColumnStates', () => {
    it('should return correct states for pending column', () => {
      const result = getColumnStates('pending');
      expect(result).toEqual(['DRAFT', 'PENDING_CHECK']);
    });

    it('should return correct states for needs-attention column', () => {
      const result = getColumnStates('needs-attention');
      expect(result).toEqual(['CHECK_FAILED', 'PENDING_METADATA']);
    });

    it('should return correct states for ready column', () => {
      const result = getColumnStates('ready');
      expect(result).toEqual(['MATCH_PENDING', 'MATCHED']);
    });

    it('should return correct states for processing column', () => {
      const result = getColumnStates('processing');
      expect(result).toEqual(['PENDING_ARCHIVE', 'PENDING_APPROVAL', 'ARCHIVING', 'ARCHIVED']);
    });

    it('should return empty array for unknown column', () => {
      const result = getColumnStates('unknown');
      expect(result).toEqual([]);
    });
  });
});
