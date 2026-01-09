// Input: vitest、路由路径常量
// Output: 路由映射测试
// Pos: src/routes/__tests__/paths.test.ts

import { describe, it, expect } from 'vitest';
import { SUBITEM_TO_PATH } from '@/routes/paths';

describe('routes paths mapping', () => {
  it('银行回单子菜单应映射到 BANK_RECEIPT 类型', () => {
    expect(SUBITEM_TO_PATH['银行回单']).toBe('/system/archive/original-vouchers?type=BANK_RECEIPT');
  });
});
