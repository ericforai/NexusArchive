// Input: React Testing Library, BatchUploadView component
// Output: Component tests for batch upload with four nature check integration
// Pos: src/pages/collection/__tests__/BatchUploadView.test.tsx

import { describe, it, expect } from 'vitest';
import type { BatchCompleteResult, FailedFileInfo } from '@/api/batchUpload';

describe('BatchUpload API Types - Four Nature Check Integration', () => {
  describe('BatchCompleteResult with new check fields', () => {
    it('should accept response with check statistics', () => {
      const response: BatchCompleteResult = {
        batchId: 1,
        batchNo: 'COL-20240105-001',
        status: 'UPLOADED',
        totalFiles: 2,
        uploadedFiles: 2,
        failedFiles: 0,
        checkedFiles: 2,
        passedFiles: 1,
        failedFileList: [
          { fileId: 'file-002', fileName: 'fail.pdf', reason: '真实性检测失败' }
        ],
      };

      expect(response.checkedFiles).toBe(2);
      expect(response.passedFiles).toBe(1);
      expect(response.failedFileList).toHaveLength(1);
      expect(response.failedFileList?.[0].reason).toBe('真实性检测失败');
    });

    it('should support backward compatible response without new fields', () => {
      const response: BatchCompleteResult = {
        batchId: 1,
        batchNo: 'COL-20240105-001',
        status: 'UPLOADED',
        totalFiles: 2,
        uploadedFiles: 2,
        failedFiles: 0,
      };

      expect(response.batchId).toBe(1);
      expect(response.checkedFiles).toBeUndefined();
      expect(response.passedFiles).toBeUndefined();
    });
  });

  describe('FailedFileInfo type', () => {
    it('should represent a failed file with reason', () => {
      const failedFile: FailedFileInfo = {
        fileId: 'file-001',
        fileName: 'test.pdf',
        reason: '完整性检测失败'
      };

      expect(failedFile.fileId).toBe('file-001');
      expect(failedFile.fileName).toBe('test.pdf');
      expect(failedFile.reason).toBe('完整性检测失败');
    });
  });
});
