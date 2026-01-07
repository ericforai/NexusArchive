// Input: 无外部依赖
// Output: 批量操作组件模块清单
// Pos: src/components/operations/manifest.config.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

export const moduleManifest = {
  id: 'component.operations',
  owner: 'team-archive',
  publicApi: './index.ts',
  canImportFrom: [
    'src/api/**',
    'src/utils/**',
    'src/hooks/**',
    'src/lib/**'
  ],
  restrictions: {
    disallowDeepImport: true
  },
  tags: ['batch', 'operations', 'approval']
};
