// Input: API client 与后端 ScanWorkspace/OcrResult 类型映射
// Output: scanApi 对象与扫描工作区接口定义
// Pos: 扫描工作区 API 客户端

import { client } from './client';
import { ApiResponse } from '../types';

/**
 * 扫描工作区项
 */
export interface ScanWorkspaceItem {
  id?: number;
  sessionId?: string;
  userId: string;
  fileName: string;
  filePath: string;
  fileSize: number;
  fileType: string;
  uploadSource: string;
  ocrStatus: 'pending' | 'processing' | 'review' | 'completed' | 'failed';
  ocrEngine?: string;
  ocrResult?: string;
  overallScore?: number;
  docType?: string;
  submitStatus: 'draft' | 'submitted';
  archiveId?: string;
  submittedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * OCR 识别字段
 */
export interface OcrField {
  label: string;
  value: string;
  confidence?: number;
  editable?: boolean;
}

/**
 * OCR 识别响应
 */
export interface OcrResponse {
  success: boolean;
  docType?: string;
  invoiceNumber?: string;
  invoiceDate?: string;
  amount?: number;
  taxAmount?: number;
  totalAmount?: number;
  sellerName?: string;
  sellerTaxId?: string;
  buyerName?: string;
  buyerTaxId?: string;
  engine?: string;
  confidence?: number;
  rawText?: string;
  extraFields?: Record<string, unknown>;
  errorMessage?: string;
}

/**
 * 提交结果
 */
export interface ScanSubmitResult {
  archiveId: string;
  message: string;
}

/**
 * 扫描工作区 API
 */
export const scanApi = {
  /**
   * 获取当前用户的工作区文件列表
   * GET /api/scan/workspace
   */
  getWorkspace: async () => {
    const response = await client.get<ApiResponse<ScanWorkspaceItem[]>>('/scan/workspace');
    return response.data;
  },

  /**
   * 上传文件到工作区
   * POST /api/scan/workspace/upload
   */
  upload: async (file: File, source: string = 'upload', sessionId?: string) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('uploadSource', source);
    if (sessionId) {
      formData.append('sessionId', sessionId);
    }

    const response = await client.post<ApiResponse<ScanWorkspaceItem>>(
      '/scan/workspace/upload',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data;
  },

  /**
   * 触发 OCR 识别
   * POST /api/scan/workspace/{id}/ocr
   */
  processOcr: async (id: number, engine?: string) => {
    const params = engine ? { engine } : {};
    const response = await client.post<ApiResponse<void>>(
      `/scan/workspace/${id}/ocr`,
      null,
      { params }
    );
    return response.data;
  },

  /**
   * 更新工作区数据
   * PUT /api/scan/workspace/{id}
   */
  update: async (id: number, data: Partial<ScanWorkspaceItem>) => {
    const response = await client.put<ApiResponse<ScanWorkspaceItem>>(
      `/scan/workspace/${id}`,
      data
    );
    return response.data;
  },

  /**
   * 提交到预归档池
   * POST /api/scan/workspace/{id}/submit
   */
  submit: async (id: number) => {
    const response = await client.post<ApiResponse<ScanSubmitResult>>(
      `/scan/workspace/${id}/submit`
    );
    return response.data;
  },

  /**
   * 删除工作区项
   * DELETE /api/scan/workspace/{id}
   */
  delete: async (id: number) => {
    const response = await client.delete<ApiResponse<void>>(`/scan/workspace/${id}`);
    return response.data;
  },

  /**
   * 创建移动端扫描会话
   * POST /api/scan/mobile/session
   */
  createSession: async () => {
    const response = await client.post<ApiResponse<{ sessionId: string }>>(
      '/scan/mobile/session'
    );
    return response.data;
  },
};
