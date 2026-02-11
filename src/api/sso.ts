// Input: API client、ApiResponse 类型
// Output: ssoApi
// Pos: ERP SSO API 层

import { client } from './client';
import { ApiResponse } from '../types';
import type { UserInfo } from './auth';

export interface ConsumeTicketResponse {
  token: string;
  user: UserInfo;
  voucherNo: string;
}

export const ssoApi = {
  consume: async (ticket: string) => {
    const response = await client.post<ApiResponse<ConsumeTicketResponse>>(`/erp/sso/consume?ticket=${encodeURIComponent(ticket)}`);
    return response.data;
  },
};
