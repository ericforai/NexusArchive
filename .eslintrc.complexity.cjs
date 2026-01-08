// Input: ESLint、TypeScript 编译器
// Output: 前端代码复杂度检查配置
// Pos: ESLint 复杂度规则配置文件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * ESLint 复杂度规则配置 (Flat Config Format for ESLint 9)
 *
 * 此配置专注于检测代码复杂度违规，独立于常规 ESLint 检查。
 *
 * 使用方式:
 *   npm run complexity:check    # 检查所有源文件
 *   npm run complexity:report   # 生成详细报告
 *
 * 规则说明:
 *   - max-lines: 单文件最大行数 (300普通文件, 600页面文件)
 *   - max-lines-per-function: 单函数最大行数 (50)
 *   - max-depth: 最大嵌套深度 (4)
 *   - max-params: 最大参数数量 (10)
 *   - complexity: 圈复杂度 (10)
 */
const tseslint = require('@typescript-eslint/eslint-plugin');
const tsParser = require('@typescript-eslint/parser');

module.exports = [
    {
        files: ['src/**/*.ts', 'src/**/*.tsx'],
        languageOptions: {
            parser: tsParser,
            parserOptions: {
                ecmaVersion: 'latest',
                sourceType: 'module',
                ecmaFeatures: {
                    jsx: true,
                },
            },
            globals: {
                // Browser globals
                window: 'readonly',
                document: 'readonly',
                console: 'readonly',
                navigator: 'readonly',
                localStorage: 'readonly',
                sessionStorage: 'readonly',
                fetch: 'readonly',
                Request: 'readonly',
                Response: 'readonly',
                URL: 'readonly',
                URLSearchParams: 'readonly',
                Blob: 'readonly',
                File: 'readonly',
                FileReader: 'readonly',
                WebSocket: 'readonly',
                MouseEvent: 'readonly',
                KeyboardEvent: 'readonly',
                Event: 'readonly',
                CustomEvent: 'readonly',
                FormData: 'readonly',
                AbortController: 'readonly',
                AbortSignal: 'readonly',
                // Node globals (for build scripts)
                process: 'readonly',
                Buffer: 'readonly',
                __dirname: 'readonly',
                __filename: 'readonly',
                module: 'readonly',
                require: 'readonly',
                exports: 'readonly',
                // React globals
                React: 'readonly',
            },
        },
        plugins: {
            '@typescript-eslint': tseslint,
        },
        // 忽略源代码中针对其他 ESLint 插件的禁用注释
        linterOptions: {
            reportUnusedDisableDirectives: false,
        },
        rules: {
            // ========================================
            // 📏 文件长度规则
            // ========================================

            // 单文件最大行数 (默认 300)
            'max-lines': [
                'warn',
                {
                    max: 300,
                    skipBlankLines: true,
                    skipComments: true,
                },
            ],

            // ========================================
            // 🎯 函数复杂度规则
            // ========================================

            // 单函数最大行数 (不包括空行和注释)
            'max-lines-per-function': [
                'warn',
                {
                    max: 50,
                    skipBlankLines: true,
                    skipComments: true,
                },
            ],

            // 最大嵌套深度 (回调地狱检测)
            'max-depth': ['warn', 4],

            // 最大参数数量
            'max-params': ['warn', 10],

            // 圈复杂度 ( McCabe 复杂度)
            'complexity': ['warn', 10],

            // 最大嵌套回调数
            'max-nested-callbacks': ['warn', 4],

            // ========================================
            // TypeScript 特定规则
            // ========================================

            // 禁止 any 类型 (复杂度相关)
            '@typescript-eslint/no-explicit-any': 'off',

            // 禁止未使用的变量
            '@typescript-eslint/no-unused-vars': ['warn', {
                argsIgnorePattern: '^_',
                varsIgnorePattern: '^_',
            }],

            // ========================================
            // 其他代码质量规则
            // ========================================

            // 禁止 console (复杂度代码中常出现调试代码)
            'no-console': ['warn', { allow: ['warn', 'error'] }],

            // 禁止 debugger
            'no-debugger': 'error',

            // 禁止重复的函数参数
            'no-dupe-args': 'error',

            // 禁止重复的类成员
            'no-dupe-keys': 'error',

            // 禁止重复的 case 标签
            'no-duplicate-case': 'error',

            // 禁止空语句块
            'no-empty': 'warn',

            // 禁止不必要的嵌套块
            'no-lone-blocks': 'warn',

            // 禁止多个空行
            'no-multiple-empty-lines': ['warn', { max: 2, maxEOF: 1 }],

            // 禁止不必要的分号
            'no-extra-semi': 'warn',

            // 强制一致的分号使用
            'semi': ['warn', 'always'],
        },
    },

    // ========================================
    // 覆盖规则 (页面组件允许更长)
    // ========================================

    {
        files: ['src/pages/**/*.tsx', 'src/pages/**/*.ts'],
        rules: {
            'max-lines': [
                'warn',
                {
                    max: 600,
                    skipBlankLines: true,
                    skipComments: true,
                },
            ],
        },
    },

    {
        files: ['**/*.test.ts', '**/*.test.tsx', '**/__tests__/**/*.ts', '**/__tests__/**/*.tsx'],
        rules: {
            'max-lines': 'off',
            'max-lines-per-function': 'off',
            'max-depth': 'off',
        },
    },

    {
        files: ['*.config.ts', '*.config.js', '*.config.cjs', 'vite.config.ts', 'eslint.config.*'],
        rules: {
            'max-lines': 'off',
            'complexity': 'off',
        },
    },

    // 忽略 node_modules 和构建产物
    {
        ignores: [
            '**/node_modules/**',
            '**/dist/**',
            '**/build/**',
            '**/.next/**',
            '**/coverage/**',
        ],
    },
];
