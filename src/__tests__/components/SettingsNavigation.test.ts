// Input: vitest、导航常量、路由路径常量
// Output: 系统设置菜单路径测试用例
// Pos: 前端单元测试 - 组件级
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { describe, it, expect } from 'vitest';
import { NAV_ITEMS } from '@/constants';
import { ViewState } from '@/types';
import { ROUTE_PATHS } from '@/routes/paths';

describe('系统设置菜单路径', () => {
    it('用户权限应指向用户管理页面路径', () => {
        const settings = NAV_ITEMS.find(item => item.id === ViewState.SETTINGS);
        const userItem = settings?.children?.find(child => child.label === '用户权限');
        expect(userItem?.path).toBe(ROUTE_PATHS.SETTINGS_USERS);
    });
});
