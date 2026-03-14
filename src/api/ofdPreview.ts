// Input: API client
// Output: OFD 预览资源 API
// Pos: 前端 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';

export interface OfdPreviewResource {
  preferredMode: 'converted' | 'liteofd';
  originalFileId: string;
  originalDownloadUrl: string;
  convertedFileId?: string | null;
  convertedMimeType?: string | null;
  convertedPreviewUrl?: string | null;
  fileName?: string | null;
}

export const ofdPreviewApi = {
  async getResource(fileId: string): Promise<OfdPreviewResource> {
    const { data } = await client.get(`/ofd/preview-resource/${fileId}`);
    return data?.data || data;
  },
};
