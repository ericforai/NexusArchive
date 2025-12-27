// Input: ESLint, TypeScript 编译器
// Output: 前端代码质量与架构边界强制执行配置
// Pos: ESLint 配置文件根目录
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

module.exports = {
    root: true,
    env: {
        browser: true,
        es2021: true,
        node: true,
    },
    extends: [
        'eslint:recommended',
        'plugin:@typescript-eslint/recommended',
        'plugin:react/recommended',
        'plugin:react-hooks/recommended',
    ],
    parser: '@typescript-eslint/parser',
    parserOptions: {
        ecmaVersion: 'latest',
        sourceType: 'module',
        ecmaFeatures: {
            jsx: true,
        },
    },
    plugins: ['@typescript-eslint', 'react', 'react-hooks', 'boundaries'],
    settings: {
        react: {
            version: 'detect',
        },
        'boundaries/elements': [
            // Feature entrypoints
            { type: 'settings-feature-interface', pattern: 'src/features/settings/index.ts', mode: 'file' },
            { type: 'feature-interface', pattern: 'src/features/*/index.ts', mode: 'file' },
            { type: 'feature-impl', pattern: 'src/features/*/**/*', mode: 'file' },

            // Pages
            { type: 'page-settings', pattern: 'src/pages/settings/**/*', mode: 'file' },
            { type: 'page', pattern: 'src/pages/**/*', mode: 'file' },

            // Routing & layout
            { type: 'route', pattern: 'src/routes/**/*', mode: 'file' },
            { type: 'layout', pattern: 'src/layouts/**/*', mode: 'file' },
            { type: 'auth', pattern: 'src/auth/**/*', mode: 'file' },

            // UI
            { type: 'ui-common', pattern: 'src/components/common/**/*', mode: 'file' },
            { type: 'ui', pattern: 'src/components/**/*', mode: 'file' },

            // Infra & shared
            { type: 'api', pattern: 'src/api/**/*', mode: 'file' },
            { type: 'store', pattern: 'src/store/**/*', mode: 'file' },
            { type: 'shared', pattern: 'src/utils/**/*', mode: 'file' },
            { type: 'shared', pattern: 'src/hooks/**/*', mode: 'file' },
            { type: 'shared', pattern: 'src/types.ts', mode: 'file' },
            { type: 'shared', pattern: 'src/constants.tsx', mode: 'file' },
            { type: 'shared', pattern: 'src/queryClient.ts', mode: 'file' },
        ],
    },
    rules: {
        '@typescript-eslint/no-explicit-any': 'off',
        'react/react-in-jsx-scope': 'off',
        'react/prop-types': 'off',

        // ========================================
        // 🧱 Module Boundaries (eslint-plugin-boundaries)
        // @see /docs/architecture/frontend-boundaries.md
        // ========================================
        'boundaries/element-types': [
            'error',
            {
                default: 'allow',
                rules: [
                    // Components: no features/pages/api/store
                    {
                        from: ['ui', 'ui-common'],
                        disallow: [
                            'feature-impl',
                            'feature-interface',
                            'settings-feature-interface',
                            'page',
                            'page-settings',
                            'api',
                            'store',
                        ],
                        message: 'components 不得依赖 features/pages/api/store (模块边界规则)',
                    },

                    // Pages: no deep feature imports
                    {
                        from: ['page', 'page-settings'],
                        disallow: ['feature-impl'],
                        message: 'pages 只能从 features/<module>/index.ts 引入 (禁止深路径)',
                    },

                    // Settings pages: only allow settings entrypoint
                    {
                        from: ['page-settings'],
                        disallow: ['feature-interface'],
                        message: 'settings pages 仅允许从 features/settings 入口引入',
                    },

                    // Routes: only pages + layouts/common/auth
                    {
                        from: ['route'],
                        disallow: [
                            'feature-impl',
                            'feature-interface',
                            'settings-feature-interface',
                            'api',
                            'store',
                            'ui',
                        ],
                        message: 'routes 仅可引入 pages + layouts/common/auth',
                    },
                ],
            },
        ],
    },
};
