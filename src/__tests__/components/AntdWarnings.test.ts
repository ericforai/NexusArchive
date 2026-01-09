// Input: vitest、fs、path
// Output: antd 用法告警回归测试
// Pos: 前端单元测试 - 组件级
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { describe, it, expect } from 'vitest';
import fs from 'fs';
import path from 'path';

const readFile = (relativePath: string) => {
    return fs.readFileSync(path.resolve(process.cwd(), relativePath), 'utf-8');
};

describe('antd 用法告警回归', () => {
    it('不再使用 Tabs.TabPane', () => {
        const content = readFile('src/components/pool-kanban/KanbanColumn.tsx');
        expect(content).not.toMatch(/Tabs\.TabPane/);
    });

    it('Spin tip 不使用自闭合写法', () => {
        const files = [
            'src/components/pool-kanban/PoolKanbanView.tsx',
            'src/pages/panorama/VoucherPreviewDrawer.tsx',
            'src/pages/operations/archive-batch/components/BatchTable.tsx',
            'src/pages/archives/ArchiveDetailDrawer.tsx',
            'src/pages/matching/ComplianceReport.tsx',
            'src/pages/matching/VoucherMatchingView.tsx',
        ];
        const selfClosingTipPattern = /<Spin[^>]*\btip=[^>]*\/>/;
        files.forEach((file) => {
            const content = readFile(file);
            expect(content).not.toMatch(selfClosingTipPattern);
        });
    });
});
