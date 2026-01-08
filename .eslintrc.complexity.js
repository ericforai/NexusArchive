/**
 * ESLint Complexity Rules for NexusArchive
 *
 * Purpose: Enforce code complexity limits to maintain maintainability
 * Input: N/A (configuration file)
 * Pos: project root
 */
import tsParser from '@typescript-eslint/parser';

export default [
  {
    files: ['src/**/*.ts', 'src/**/*.tsx'],
    languageOptions: {
      parser: tsParser,
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
