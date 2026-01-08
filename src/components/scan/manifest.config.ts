// Input: 无外部依赖
// Output: 扫描集成模块清单
// Pos: src/components/scan/manifest.config.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

export const moduleManifest = {
  id: 'component.scan',
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
  tags: ['scan', 'workspace', 'folder-monitor']
};
