// Input: TypeScript 编译器、ESLint 配置
// Output: 代码复杂度检查配置（圈复杂度、嵌套深度、函数行数等）
// Pos: ESLint 配置文件根目录
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

module.exports = [
  {
    files: ['src/**/*.ts', 'src/**/*.tsx'],
    languageOptions: {
      parser: require('@typescript-eslint/parser'),
      parserOptions: {
        ecmaVersion: 'latest',
        sourceType: 'module',
        ecmaFeatures: {
          jsx: true
        },
        project: './tsconfig.json'
      },
      globals: {
        // Browser globals
        'window': 'readonly',
        'document': 'readonly',
        'console': 'readonly',
        'Blob': 'readonly',
        'File': 'readonly',
        'FormData': 'readonly',
        'URLSearchParams': 'readonly',
        'HTMLElement': 'readonly',
        'HTMLInputElement': 'readonly',
        'localStorage': 'readonly',
        'sessionStorage': 'readonly',
        'navigator': 'readonly',
        'fetch': 'readonly',
        'URL': 'readonly',
        // Test globals
        'describe': 'readonly',
        'it': 'readonly',
        'test': 'readonly',
        'expect': 'readonly',
        'beforeEach': 'readonly',
        'afterEach': 'readonly',
        'beforeAll': 'readonly',
        'afterAll': 'readonly',
        'vi': 'readonly',
        'global': 'readonly'
      }
    },
    rules: {
      // Disable non-complexity rules
      'no-undef': 'off',
      'no-unused-vars': 'off',

      // === COMPLEXITY RULES ===

      // Maximum lines per file (excluding blank lines and comments)
      'max-lines': ['warn', {
        max: 300,
        skipBlankLines: true,
        skipComments: true
      }],
      // Maximum lines per function
      'max-lines-per-function': ['warn', {
        max: 50,
        skipBlankLines: true,
        skipComments: true
      }],
      // Maximum nesting depth
      'max-depth': ['warn', 4],
      // Maximum function parameters
      'max-params': ['warn', 10],
      // Cyclomatic complexity
      'complexity': ['warn', 10],
      // Maximum nested callbacks
      'max-nested-callbacks': ['warn', 4]
    }
  },
  {
    // Pages are allowed to be larger (600 lines)
    files: ['src/pages/**/*.tsx'],
    rules: {
      'max-lines': ['warn', {
        max: 600,
        skipBlankLines: true,
        skipComments: true
      }]
    }
  }
];
