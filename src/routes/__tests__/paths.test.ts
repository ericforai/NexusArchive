// Input: vitest、路由路径常量
// Output: 路由映射测试
// Pos: src/routes/__tests__/paths.test.ts

import { describe, it, expect } from 'vitest';
import { SUBITEM_TO_PATH } from '@/routes/paths';

describe('routes paths mapping', () => {
  it('银行回单子菜单应映射到 BANK_RECEIPT 类型', () => {
    expect(SUBITEM_TO_PATH['银行回单']).toBe('/system/archive/original-vouchers?type=BANK_RECEIPT');
  });

  it('批量上传子菜单应映射到 collection/upload', () => {
    expect(SUBITEM_TO_PATH['批量上传']).toBe('/system/collection/upload');
  });

  it('原始凭证三级菜单应映射到对应类型筛选', () => {
    expect(SUBITEM_TO_PATH['销售订单']).toBe('/system/archive/original-vouchers?type=SALES_ORDER');
    expect(SUBITEM_TO_PATH['付款申请单']).toBe('/system/archive/original-vouchers?type=PAYMENT_REQ');
    expect(SUBITEM_TO_PATH['增值税专票']).toBe('/system/archive/original-vouchers?type=VAT_INVOICE');
  });

  it('单据池财政票类型码应为 INV_GOV', () => {
    expect(SUBITEM_TO_PATH['单据池:数电票（财政）']).toBe('/system/pre-archive/doc-pool?type=INV_GOV');
  });
});
