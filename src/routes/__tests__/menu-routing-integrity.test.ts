// Input: vitest、react-router-dom matchRoutes、菜单常量与路由配置
// Output: 二级/三级菜单路由完整性测试
// Pos: src/routes/__tests__/menu-routing-integrity.test.ts

import { describe, it, expect } from 'vitest';
import { matchRoutes } from 'react-router-dom';
import { NAV_ITEMS } from '@/constants';
import { routes } from '@/routes';
import { ROUTE_PATHS, SUBITEM_TO_PATH } from '@/routes/paths';
import { NavItem, ViewState } from '@/types';

const VIEW_TO_PATH: Record<string, string> = {
  [ViewState.PORTAL]: ROUTE_PATHS.PORTAL,
  [ViewState.PANORAMA]: ROUTE_PATHS.PANORAMA,
  [ViewState.PRE_ARCHIVE]: ROUTE_PATHS.PRE_ARCHIVE,
  [ViewState.COLLECTION]: ROUTE_PATHS.COLLECTION,
  [ViewState.ACCOUNT_ARCHIVES]: ROUTE_PATHS.ARCHIVE,
  [ViewState.ARCHIVE_OPS]: ROUTE_PATHS.ARCHIVE_OPS,
  [ViewState.ARCHIVE_UTILIZATION]: ROUTE_PATHS.ARCHIVE_UTILIZATION,
  [ViewState.WAREHOUSE]: ROUTE_PATHS.WAREHOUSE,
  [ViewState.STATS]: ROUTE_PATHS.STATS,
  [ViewState.QUALITY]: ROUTE_PATHS.QUALITY,
  [ViewState.SETTINGS]: ROUTE_PATHS.SETTINGS,
  [ViewState.ADMIN]: ROUTE_PATHS.ADMIN,
  [ViewState.DESTRUCTION]: ROUTE_PATHS.DESTRUCTION,
  [ViewState.LANDING]: '/',
  [ViewState.ABNORMAL]: ROUTE_PATHS.PRE_ARCHIVE_ABNORMAL,
  [ViewState.COMPLIANCE_REPORT]: ROUTE_PATHS.ARCHIVE,
  [ViewState.MATCHING]: '/system/matching',
};

const resolveMainPath = (item: NavItem): string => {
  if (VIEW_TO_PATH[item.id]) return VIEW_TO_PATH[item.id];
  if (item.path && SUBITEM_TO_PATH[item.path]) return SUBITEM_TO_PATH[item.path];
  if (item.path) return item.path;
  return '#';
};

interface FlattenedItem {
  depth: number;
  chain: string;
  item: NavItem;
}

const flattenNavItems = (items: NavItem[], depth = 0, parentChain = ''): FlattenedItem[] => {
  return items.flatMap(item => {
    const chain = parentChain ? `${parentChain} > ${item.label}` : item.label;
    const current: FlattenedItem = { depth, chain, item };
    const children = item.children ? flattenNavItems(item.children, depth + 1, chain) : [];
    return [current, ...children];
  });
};

describe('menu routing integrity', () => {
  it('所有二级/三级叶子菜单应命中有效路由而非首页兜底', () => {
    const allItems = flattenNavItems(NAV_ITEMS);
    const submenuLeaves = allItems.filter(entry => entry.depth >= 1 && !entry.item.children?.length);

    for (const entry of submenuLeaves) {
      const { chain, item } = entry;
      const resolved = resolveMainPath(item);

      expect(resolved, `${chain} 未配置跳转路径`).not.toBe('#');
      expect(resolved.startsWith('/'), `${chain} 的路径必须是绝对路径`).toBe(true);

      const pathname = resolved.split('?')[0];
      const matches = matchRoutes(routes, pathname);
      expect(matches?.length, `${chain} 无法命中任何路由：${pathname}`).toBeGreaterThan(0);

      const finalMatchPath = matches?.[matches.length - 1]?.route.path;
      expect(finalMatchPath, `${chain} 落入全局 * 路由：${pathname}`).not.toBe('*');
    }
  });
});
