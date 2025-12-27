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
    plugins: ['@typescript-eslint', 'react', 'react-hooks'],
    settings: {
        react: {
            version: 'detect',
        },
    },
    rules: {
        '@typescript-eslint/no-explicit-any': 'warn',
        'react/react-in-jsx-scope': 'off',
        'react/prop-types': 'off',
    },

    // ========================================
    // 🏛 架构边界强制规则
    // @see /docs/architecture/frontend-boundaries.md
    // ========================================
    overrides: [
        // 规则 A: components/common + components/layout 纯 UI 层禁止依赖业务逻辑
        {
            files: [
                'src/components/common/**/*.{ts,tsx}',
                'src/components/layout/**/*.{ts,tsx}',
            ],
            rules: {
                'no-restricted-imports': [
                    'error',
                    {
                        patterns: [
                            {
                                group: ['**/features/**'],
                                message:
                                    '❌ components/common 禁止依赖 features/* (违反架构边界规则 A)',
                            },
                            {
                                group: ['**/api/**'],
                                message:
                                    '❌ components/common 禁止依赖 api/* (违反架构边界规则 A)',
                            },
                            {
                                group: ['**/store/**'],
                                message:
                                    '❌ components/common 禁止依赖 store/* (违反架构边界规则 A)',
                            },
                            {
                                group: [
                                    '**/components/archive/**',
                                    '**/components/matching/**',
                                    '**/components/settings/**',
                                    '**/components/admin/**',
                                ],
                                message:
                                    '❌ components/common 禁止依赖页面组件 (违反架构边界规则 A)',
                            },
                        ],
                    },
                ],
            },
        },

        // 规则 C: features/* 领域逻辑层禁止反向依赖 components
        {
            files: ['src/features/**/*.{ts,tsx}'],
            rules: {
                'no-restricted-imports': [
                    'error',
                    {
                        patterns: [
                            {
                                group: ['**/components/**'],
                                message:
                                    '❌ features/* 禁止依赖 components/* (违反架构边界规则 C - 防止反向依赖)',
                            },
                        ],
                    },
                ],
            },
        },
    ],
};
