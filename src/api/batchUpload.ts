// Input: axios, client 与本地类型
// Output: Batch Upload API Client
// Pos: API Layer - 批量上传接口

import { client } from './client';

// ===== Types =====

/**
 * API响应格式
 */
interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

/**
 * 批量上传请求
 */
export interface BatchUploadRequest {
  batchName: string;
  fondsCode: string;
  fiscalYear: string;
  fiscalPeriod?: string;
  archivalCategory: 'VOUCHER' | 'LEDGER' | 'REPORT' | 'OTHER';
  totalFiles: number;
  autoCheck?: boolean;
}

/**
 * 批量上传响应
 */
export interface BatchUploadResponse {
  batchId: number;
  batchNo: string;
  status: string;
  uploadToken: string;
  totalFiles: number;
  uploadedFiles: number;
  failedFiles: number;
  progress: number;
  recentFiles?: FileInfo[];
}

/**
 * 文件信息
 */
export interface FileInfo {
  originalFilename: string;
  uploadStatus: string;
  fileSizeBytes: number;
  errorMessage?: string;
}

/**
 * 批次详情响应
 */
export interface BatchDetailResponse {
  id: number;
  batchNo: string;
  batchName: string;
  fondsCode: string;
  fiscalYear: string;
  archivalCategory: string;
  status: string;
  totalFiles: number;
  uploadedFiles: number;
  failedFiles: number;
  totalSizeBytes: number;
  progress: number;
}

/**
 * 批次文件响应
 */
export interface BatchFileResponse {
  id: number;
  fileId?: string;
  originalFilename: string;
  uploadStatus: string;
  fileSizeBytes: number;
  errorMessage?: string;
}

/**
 * 文件上传结果
 */
export interface FileUploadResult {
  fileId: string;
  originalFilename: string;
  status: string;
  errorMessage?: string;
}

/**
 * 批次完成结果
 */
export interface BatchCompleteResult {
  batchId: number;
  batchNo: string;
  status: string;
  totalFiles: number;
  uploadedFiles: number;
  failedFiles: number;
}

/**
 * 批次检测结果
 */
export interface BatchCheckResult {
  batchId: number;
  totalFiles: number;
  checkedFiles: number;
  passedFiles: number;
  failedFiles: number;
  summary: string;
}

/**
 * 上传进度回调
 */
export type UploadProgressCallback = (progress: number) => void;

// ===== API Client =====

/**
 * 批量上传 API
 */
export const batchUploadApi = {
  /**
   * 创建上传批次
   *
   * POST /api/collection/batch/create
   */
  async createBatch(request: BatchUploadRequest): Promise<BatchUploadResponse> {
    const response = await client.post<ApiResponse<BatchUploadResponse>>(
      '/collection/batch/create',
      request
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message || '创建批次失败');
  },

  /**
   * 上传单个文件
   *
   * POST /api/collection/batch/{batchId}/upload
   */
  async uploadFile(
    batchId: number,
    file: File,
    onProgress?: UploadProgressCallback
  ): Promise<FileUploadResult> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await client.post<ApiResponse<FileUploadResult>>(
      `/collection/batch/${batchId}/upload`,
      formData,
      {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (progressEvent) => {
          if (onProgress && progressEvent.total) {
            const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            onProgress(progress);
          }
        },
      }
    );

    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message || '文件上传失败');
  },

  /**
   * 完成批次上传
   *
   * POST /api/collection/batch/{batchId}/complete
   */
  async completeBatch(batchId: number): Promise<BatchCompleteResult> {
    const response = await client.post<ApiResponse<BatchCompleteResult>>(
      `/collection/batch/${batchId}/complete`
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message || '完成批次失败');
  },

  /**
   * 取消批次
   *
   * POST /api/collection/batch/{batchId}/cancel
   */
  async cancelBatch(batchId: number): Promise<void> {
    const response = await client.post<ApiResponse<void>>(
      `/collection/batch/${batchId}/cancel`
    );
    if (response.data.code !== 200) {
      throw new Error(response.data.message || '取消批次失败');
    }
  },

  /**
   * 获取批次详情
   *
   * GET /api/collection/batch/{batchId}
   */
  async getBatchDetail(batchId: number): Promise<BatchDetailResponse> {
    const response = await client.get<ApiResponse<BatchDetailResponse>>(
      `/collection/batch/${batchId}`
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message || '获取批次详情失败');
  },

  /**
   * 获取批次文件列表
   *
   * GET /api/collection/batch/{batchId}/files
   */
  async getBatchFiles(batchId: number): Promise<BatchFileResponse[]> {
    const response = await client.get<ApiResponse<BatchFileResponse[]>>(
      `/collection/batch/${batchId}/files`
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message || '获取文件列表失败');
  },

  /**
   * 执行四性检测
   *
   * POST /api/collection/batch/{batchId}/check
   */
  async runFourNatureCheck(batchId: number): Promise<BatchCheckResult> {
    const response = await client.post<ApiResponse<BatchCheckResult>>(
      `/collection/batch/${batchId}/check`
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message || '执行检测失败');
  },

  /**
   * 获取批次列表
   *
   * GET /api/collection/batch/list
   */
  async listBatches(limit = 20, offset = 0): Promise<BatchDetailResponse[]> {
    const response = await client.get<ApiResponse<BatchDetailResponse[]>>(
      '/collection/batch/list',
      { params: { limit, offset } }
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message || '获取批次列表失败');
  },
};

// ===== 状态常量 =====

/**
 * 批次状态
 */
export const BatchStatus = {
  UPLOADING: 'UPLOADING',
  UPLOADED: 'UPLOADED',
  VALIDATING: 'VALIDATING',
  VALIDATED: 'VALIDATED',
  FAILED: 'FAILED',
  ARCHIVED: 'ARCHIVED',
} as const;

/**
 * 文件上传状态
 */
export const FileUploadStatus = {
  PENDING: 'PENDING',
  UPLOADING: 'UPLOADING',
  UPLOADED: 'UPLOADED',
  FAILED: 'FAILED',
  DUPLICATE: 'DUPLICATE',
  VALIDATING: 'VALIDATING',
  VALIDATED: 'VALIDATED',
  CHECK_FAILED: 'CHECK_FAILED',
} as const;

/**
 * 档案门类
 */
export const ArchivalCategory = {
  VOUCHER: 'VOUCHER',
  LEDGER: 'LEDGER',
  REPORT: 'REPORT',
  OTHER: 'OTHER',
} as const;

/**
 * 档案门类标签映射
 */
export const ArchivalCategoryLabels: Record<string, string> = {
  VOUCHER: '会计凭证',
  LEDGER: '会计账簿',
  REPORT: '财务报告',
  OTHER: '其他资料',
};
