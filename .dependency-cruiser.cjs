/** @type {import('dependency-cruiser').IConfiguration} */
module.exports = {
  forbidden: [
    {
      name: 'no-circular',
      severity: 'error',
      comment:
        'This dependency is part of a circular relationship. You might want to revise ' +
        'your solution (i.e. use dependency inversion, make sure the modules have a single responsibility) ',
      from: {},
      to: {
        circular: true
      }
    },
    {
      name: 'no-orphans',
      comment:
        "This is an orphan module - it's likely not used (anymore?). Either use it or " +
        "remove it. If it's logical this module is an orphan (i.e. it's a config file), " +
        "add an exception for it in your dependency-cruiser configuration. By default " +
        "this rule does not scrutinize dot-files (e.g. .eslintrc.js), TypeScript declaration " +
        "files (.d.ts), tsconfig.json and some of the babel and webpack configs.",
      severity: 'warn',
      from: {
        orphan: true,
        pathNot: [
          '(^|/)[.][^/]+[.](?:js|cjs|mjs|ts|cts|mts|json)$',                  // dot files
          '[.]d[.]ts$',                                                       // TypeScript declaration files
          '(^|/)tsconfig[.]json$',                                            // TypeScript config
          '(^|/)(?:babel|webpack)[.]config[.](?:js|cjs|mjs|ts|cts|mts|json)$', // other configs
          '(^|/)manifest[.]config[.]ts$'                                      // J1 Self-Description manifests
        ]
      },
      to: {},
    },
    {
      name: 'no-deprecated-core',
      comment:
        'A module depends on a node core module that has been deprecated. Find an alternative - these are ' +
        "bound to exist - node doesn't deprecate lightly.",
      severity: 'warn',
      from: {},
      to: {
        dependencyTypes: [
          'core'
        ],
        path: [
          '^v8/tools/codemap$',
          '^v8/tools/consarray$',
          '^v8/tools/csvparser$',
          '^v8/tools/logreader$',
          '^v8/tools/profile_view$',
          '^v8/tools/profile$',
          '^v8/tools/SourceMap$',
          '^v8/tools/splaytree$',
          '^v8/tools/tickprocessor-driver$',
          '^v8/tools/tickprocessor$',
          '^node-inspect/lib/_inspect$',
          '^node-inspect/lib/internal/inspect_client$',
          '^node-inspect/lib/internal/inspect_repl$',
          '^async_hooks$',
          '^punycode$',
          '^domain$',
          '^constants$',
          '^sys$',
          '_linklist$',
          '_stream_wrap$'
        ],
      }
    },
    {
      name: 'not-to-deprecated',
      comment:
        'This module uses a (version of an) npm module that has been deprecated. Either upgrade to a later ' +
        'version of that module or find an alternative. Deprecated modules are a security risk.',
      severity: 'warn',
      from: {},
      to: {
        dependencyTypes: [
          'deprecated'
        ]
      }
    },
    {
      name: 'no-non-package-json',
      severity: 'error',
      comment:
        "This module depends on an npm package that isn't in the 'dependencies' section of your package.json. " +
        "That's problematic as the package either (1) won't be available on live (2 - worse) will be " +
        "available on live with an non-guaranteed version. Fix it by adding the package to the dependencies " +
        "in your package.json.",
      from: {},
      to: {
        dependencyTypes: [
          'npm-no-pkg',
          'npm-unknown'
        ]
      }
    },
    {
      name: 'not-to-unresolvable',
      comment:
        "This module depends on a module that cannot be found ('resolved to disk'). If it's an npm " +
        'module: add it to your package.json. In all other cases you likely already know what to do.',
      severity: 'error',
      from: {},
      to: {
        couldNotResolve: true
      }
    },
    {
      name: 'no-duplicate-dep-types',
      comment:
        "Likely this module depends on an external ('npm') package that occurs more than once " +
        "in your package.json i.e. bot as a devDependencies and in dependencies. This will cause " +
        "maintenance problems later on.",
      severity: 'warn',
      from: {},
      to: {
        moreThanOneDependencyType: true,
        // as it's pretty common to have a type import be a type only import
        // _and_ (e.g.) a devDependency - don't consider type-only dependency
        // types for this rule
        dependencyTypesNot: ["type-only"]
      }
    },

    /* rules you might want to tweak for your specific situation: */
    {
      name: 'not-to-test',
      comment:
        "This module depends on code within a folder that should only contain tests. As tests don't " +
        "implement functionality this is odd. Either you're writing a test outside the test folder " +
        "or there's something in the test folder that isn't a test.",
      severity: 'error',
      from: {
        pathNot: '^(tests)'
      },
      to: {
        path: '^(tests)'
      }
    },
    {
      name: 'not-to-spec',
      comment:
        'This module depends on a spec (test) file. The sole responsibility of a spec file is to test code. ' +
        "If there's something in a spec that's of use to other modules, it doesn't have that single " +
        'responsibility anymore. Factor it out into (e.g.) a separate utility/ helper or a mock.',
      severity: 'error',
      from: {},
      to: {
        path: '[.](?:spec|test)[.](?:js|mjs|cjs|jsx|ts|mts|cts|tsx)$'
      }
    },
    {
      name: 'not-to-dev-dep',
      severity: 'error',
      comment:
        "This module depends on an npm package from the 'devDependencies' section of your " +
        'package.json. It looks like something that ships to production, though. To prevent problems ' +
        "with npm packages that aren't there on production declare it (only!) in the 'dependencies'" +
        'section of your package.json. If this module is development only - add it to the ' +
        'from.pathNot re of the not-to-dev-dep rule in the dependency-cruiser configuration',
      from: {
        path: '^(src)',
        // 排除测试文件和 __tests__ 目录
        pathNot: [
          '[.](?:spec|test)[.](?:js|mjs|cjs|jsx|ts|mts|cts|tsx)$',
          '^src/__tests__/',
          '^src/vite-env\\.d\\.ts$'
        ]
      },
      to: {
        dependencyTypes: [
          'npm-dev',
        ],
        // type only dependencies are not a problem as they don't end up in the
        // production code or are ignored by the runtime.
        dependencyTypesNot: [
          'type-only'
        ],
        pathNot: [
          'node_modules/@types/'
        ]
      }
    },
    {
      name: 'optional-deps-used',
      severity: 'info',
      comment:
        "This module depends on an npm package that is declared as an optional dependency " +
        "in your package.json. As this makes sense in limited situations only, it's flagged here. " +
        "If you're using an optional dependency here by design - add an exception to your" +
        "dependency-cruiser configuration.",
      from: {},
      to: {
        dependencyTypes: [
          'npm-optional'
        ]
      }
    },
    {
      name: 'peer-deps-used',
      comment:
        "This module depends on an npm package that is declared as a peer dependency " +
        "in your package.json. This makes sense if your package is e.g. a plugin, but in " +
        "other cases - maybe not so much. If the use of a peer dependency is intentional " +
        "add an exception to your dependency-cruiser configuration.",
      severity: 'warn',
      from: {},
      to: {
        dependencyTypes: [
          'npm-peer'
        ]
      }
    },
    // ============================================================
    // Architecture Defense Rules - NexusArchive Frontend
    // J2: Self-Check - Automated architecture validation
    // ============================================================

    {
      name: 'no-cross-feature-internal',
      comment:
        'Features cannot import from other features internal directories. ' +
        'Use the public API (index.ts) of the feature instead. ' +
        'Module manifest: canImportFrom should specify allowed dependencies.',
      severity: 'error',
      from: {
        path: '^src/features/[^/]+'
      },
      to: {
        path: '^src/features/([^/]+)/internal',
        pathNot: '^src/features/\\1/internal'
      }
    },

    {
      name: 'features-must-use-public-api',
      comment:
        'Features must import through index.ts public API only. ' +
        'Direct imports to feature internals violate module boundaries. ' +
        'See manifest.config.ts for the intended public API.',
      severity: 'error',
      from: {
        path: '^src/(pages|components|hooks|store)/'
      },
      to: {
        path: '^src/features/[^/]+/(?!index\\.ts).*',
        pathNot: '^src/features/[^/]+/index\\.ts$'
      }
    },

    {
      name: 'no-component-internal-import',
      comment:
        'Components should use public API exports, not internal imports. ' +
        'Import from the component\'s index.ts instead of internal/ subdirectory.',
      severity: 'error',
      from: {
        path: '^src/(pages|features)/'
      },
      to: {
        path: '^src/components/([^/]+)/internal'
      }
    },

    {
      name: 'components-must-use-public-api',
      comment:
        'Components must be imported through their index.ts public API. ' +
        'Direct imports to component internals violate encapsulation.',
      severity: 'error',
      from: {
        path: '^src/(pages|features)/'
      },
      to: {
        path: '^src/components/[^/]+/.*',
        pathNot: [
          '^src/components/[^/]+/index\\.ts$',
          '^src/components/[^/]+/index\\.tsx$'
        ]
      }
    },

    {
      name: 'api-only-in-features',
      comment:
        'Direct API calls should only be made from feature modules. ' +
        'Shared components should not make API calls directly. ' +
        'Pass data as props or use a hook instead.',
      severity: 'error',
      from: {
        path: '^src/components/(common|shared|ui)/'
      },
      to: {
        path: '^src/api/'
      }
    },

    {
      name: 'utils-only-import-from-utils-or-shared',
      comment:
        'Utility modules must remain dependency-free. ' +
        'Utils can only import from other utils or shared modules, ' +
        'not from feature-specific code (this creates hidden dependencies).',
      severity: 'error',
      from: {
        path: '^src/utils/'
      },
      to: {
        path: '^src/(features|pages)/',
        dependencyTypes: ['local']
      }
    },

    {
      name: 'hooks-only-in-features-or-pages',
      comment:
        'Custom hooks in src/hooks/ should only be used from features or pages. ' +
        'Components should not depend on feature-specific hooks. ' +
        'Exception: Layout-level components (Sidebar, GlobalSearch, pool-kanban) may use utility hooks.',
      severity: 'warn',
      from: {
        path: '^src/components/',
        pathNot: [
          '^src/components/Sidebar\\.tsx$',
          '^src/components/GlobalSearch\\.tsx$',
          '^src/components/dev/DocumentationGuardProvider\\.tsx$',
          '^src/components/pool-kanban/'
        ]
      },
      to: {
        path: '^src/hooks/'
      }
    },

    {
      name: 'store-only-import-from-features',
      comment:
        'Global store (Zustand) should only be imported from features or pages, ' +
        'not from shared components (creates hidden coupling). ' +
        'Use props or context for shared components.',
      severity: 'error',
      from: {
        path: '^src/components/(common|shared|ui)/'
      },
      to: {
        path: '^src/store/'
      }
    },

    {
      name: 'no-react-query-in-components',
      comment:
        'Shared components should not use React Query directly. ' +
        'Features/pages should handle data fetching and pass data via props.',
      severity: 'warn',
      from: {
        path: '^src/components/(common|shared|ui)/'
      },
      to: {
        path: '^src/hooks.*useQuery'
      }
    },

    {
      name: 'no-antd-theme-direct-import',
      comment:
        'Do not import theme tokens directly. Use theme tokens through CSS variables or the theme context. ' +
        'Direct imports create tight coupling to Ant Design internals.',
      severity: 'warn',
      from: {},
      to: {
        path: '^antd/es/theme/.*'
      }
    },

    {
      name: 'no-react-router-dom-in-components',
      comment:
        'Shared components should not directly depend on react-router-dom. ' +
        'Pass navigation callbacks as props instead.',
      severity: 'warn',
      from: {
        path: '^src/components/(common|shared|ui)/'
      },
      to: {
        dependencyTypes: ['npm'],
        path: '^react-router-dom'
      }
    },

    {
      name: 'no-relative-import-across-features',
      comment:
        'Use absolute imports from @/ alias instead of relative paths across features. ' +
        'Relative imports like ../../../features/xxx are brittle and break on refactoring.',
      severity: 'warn',
      from: {
        path: '^src/features/'
      },
      to: {
        path: '^\\.\\.[\\/\\\\].*\\.\\.[\\/\\\\]',
        dependencyTypes: ['local']
      }
    },

    // ============================================================
    // Batch Upload Module Rules
    // J2: Self-Check - Batch upload API architecture validation
    // ============================================================

    {
      name: 'batch-upload-api-no-internal-import',
      comment:
        'Batch upload API internal implementation cannot be imported directly. ' +
        'Use the public API (index.ts) instead. ' +
        'See: src/api/batchUpload.ts',
      severity: 'error',
      from: {
        path: '^src/(?!api/batch/)'
      },
      to: {
        path: '^src/api/batch/.*',
        pathNot: [
          '^src/api/batch/index\\.ts$',
          '^src/api/batch/.*\\.d\\.ts$'  // TypeScript definitions
        ]
      }
    },

    {
      name: 'batch-upload-restrict-deps',
      comment:
        'Batch upload API can only import declared dependencies. ' +
        'CanImportFrom: src/types.ts, src/api/client, src/utils, axios, antd' +
        'See: src/api/batchUpload.ts',
      severity: 'error',
      from: {
        path: '^src/api/batch'
      },
      to: {
        path: '^src/',
        dependencyTypes: ['local'],
        pathNot: [
          '^src/api/client\\.ts$',
          '^src/types/',
          '^src/types\\.ts$',
          '^src/utils/',
          '^src/components/',
          '^src/hooks/'
        ]
      }
    },

    // ============================================================
    // Batch Upload Compliance Components (DA/T 94-2022 Fix)
    // J2: Self-Check - Compliance fix architecture validation
    // ============================================================

    {
      name: 'batch-upload-compliance-no-internal-import',
      comment:
        'Batch upload compliance components must be imported through parent module public API. ' +
        'See: src/pages/collection/components/ComplianceAlert.tsx, ' +
        'src/pages/archives/components/ManualArchiveModal.tsx',
      severity: 'error',
      from: {
        path: '^src/(?!pages/(collection|archives))'
      },
      to: {
        path: [
          '^src/pages/collection/components/ComplianceAlert\\.tsx$',
          '^src/pages/archives/components/ManualArchiveModal\\.tsx$'
        ]
      }
    },

    {
      name: 'compliance-alert-restrict-deps',
      comment:
        'ComplianceAlert must remain dependency-free (presentational only). ' +
        'Allowed: react, antd only. No local imports. ' +
        'See: src/pages/collection/components/ComplianceAlert.tsx',
      severity: 'error',
      from: {
        path: '^src/pages/collection/components/ComplianceAlert\\.tsx$'
      },
      to: {
        path: '^src/',
        dependencyTypes: ['local']
      }
    },

    {
      name: 'manual-archive-modal-restrict-deps',
      comment:
        'ManualArchiveModal can only use: src/api/archives, src/types. ' +
        'See: src/pages/archives/components/ManualArchiveModal.tsx',
      severity: 'error',
      from: {
        path: '^src/pages/archives/components/ManualArchiveModal\\.tsx$'
      },
      to: {
        path: '^src/',
        dependencyTypes: ['local'],
        pathNot: [
          '^src/api/archives\\.ts$',
          '^src/api/types\\.ts$',
          '^src/types\\.ts$'
        ]
      }
    },

    {
      name: 'batch-upload-view-restrict-api',
      comment:
        'BatchUploadView must use collection or batchUpload API for uploads. ' +
        'See: src/pages/collection/BatchUploadView.tsx',
      severity: 'error',
      from: {
        path: '^src/pages/collection/BatchUploadView\\.tsx$'
      },
      to: {
        path: '^src/api/(?!collection|archive|batchUpload|client|types)[^/]+\\.ts$'
      }
    },

    // ============================================================
    // Integration Settings Module Rules
    // J2: Self-Check - Module-specific architecture validation
    // ============================================================

    {
      name: 'integration-settings-no-internal-import',
      comment:
        'Integration Settings internal implementation cannot be imported directly. ' +
        'Use the public API (index.ts) instead. ' +
        'See: src/components/settings/integration/manifest.config.ts',
      severity: 'error',
      from: {
        path: '^src/(?!components/settings/integration/)'
      },
      to: {
        path: '^src/components/settings/integration/(hooks|components)/(?!index\\.ts).*',
        pathNot: '^src/components/settings/integration/(hooks|components)/index\\.ts$'
      }
    },

    {
      name: 'integration-settings-restrict-deps',
      comment:
        'Integration Settings module can only import dependencies declared in manifest. ' +
        'CanImportFrom: react, antd, lucide-react, react-hot-toast, src/types.ts, src/api/**' +
        'See: src/components/settings/integration/manifest.config.ts',
      severity: 'error',
      from: {
        path: '^src/components/settings/integration/'
      },
      to: {
        path: '^src/',
        dependencyTypes: ['local'],
        pathNot: [
          '^src/types/',
          '^src/types\\.ts$',
          '^src/api/',
          '^src/components/settings/integration/'
        ]
      }
    },

    {
      name: 'integration-settings-no-db',
      comment:
        'Integration Settings (UI layer) must not directly import database models or mappers. ' +
        'Use API layer instead. See: src/components/settings/integration/manifest.config.ts',
      severity: 'error',
      from: {
        path: '^src/components/settings/integration/'
      },
      to: {
        path: '^src/(store|models|entities|mappers)/'
      }
    },

    {
      name: 'integration-settings-test-only-in-tests',
      comment:
        'Test utilities must not be imported from production code. ' +
        'Keep test dependencies in __tests__ directories only.',
      severity: 'error',
      from: {
        path: '^src/components/settings/integration/(?!__tests__|hooks/__tests__|components/__tests__)/'
      },
      to: {
        path: '^src/components/settings/integration/.*__tests__/'
      }
    }
  ],
  options: {

    /* Which modules not to follow further when encountered */
    doNotFollow: {
      /* path: an array of regular expressions in strings to match against */
      path: [
        'node_modules',
        // antd 按需导入在运行时有效，静态分析时可能无法解析
        'antd/es/.*',
        'dayjs'
      ]
    },

    /* Which modules to exclude */
    exclude: {
      /* path: an array of regular expressions in strings to match against */
      path: [
        // antd 按需导入在运行时有效，但静态分析时可能无法解析
        '^antd/es/',
        '^antd/locale/',
        // dayjs 也是有效的运行时依赖
        'dayjs'
      ]
    },

    /* Which modules to exclusively include (array of regular expressions in strings)
       dependency-cruiser will skip everything not matching this pattern
    */
    // includeOnly : [''],

    /* List of module systems to cruise.
       When left out dependency-cruiser will fall back to the list of _all_
       module systems it knows of. It's the default because it's the safe option
       It might come at a performance penalty, though.
       moduleSystems: ['amd', 'cjs', 'es6', 'tsd']

       As in practice only commonjs ('cjs') and ecmascript modules ('es6')
       are widely used, you can limit the moduleSystems to those.
     */

    // moduleSystems: ['cjs', 'es6'],

    /*
      false: don't look at JSDoc imports (the default)
      true: dependency-cruiser will detect dependencies in JSDoc-style
      import statements. Implies "parser": "tsc", so the dependency-cruiser
      will use the typescript parser for JavaScript files.

      For this to work the typescript compiler will need to be installed in the
      same spot as you're running dependency-cruiser from.
     */
    // detectJSDocImports: true,

    /*
      false: don't look at process.getBuiltinModule calls (the default)
      true: dependency-cruiser will detect calls to process.getBuiltinModule/
      globalThis.process.getBuiltinModule as imports.
     */
    detectProcessBuiltinModuleCalls: true,

    /* prefix for links in html and svg output (e.g. 'https://github.com/you/yourrepo/blob/main/'
       to open it on your online repo or `vscode://file/${process.cwd()}/` to
       open it in visual studio code),
     */
    // prefix: `vscode://file/${process.cwd()}/`,

    /* false (the default): ignore dependencies that only exist before typescript-to-javascript compilation
       true: also detect dependencies that only exist before typescript-to-javascript compilation
       "specify": for each dependency identify whether it only exists before compilation or also after
     */
    tsPreCompilationDeps: true,

    /* list of extensions to scan that aren't javascript or compile-to-javascript.
       Empty by default. Only put extensions in here that you want to take into
       account that are _not_ parsable.
    */
    // extraExtensionsToScan: [".json", ".jpg", ".png", ".svg", ".webp"],

    /* if true combines the package.jsons found from the module up to the base
       folder the cruise is initiated from. Useful for how (some) mono-repos
       manage dependencies & dependency definitions.
     */
    // combinedDependencies: false,

    /* if true leave symlinks untouched, otherwise use the realpath */
    // preserveSymlinks: false,

    /* TypeScript project file ('tsconfig.json') to use for
       (1) compilation and
       (2) resolution (e.g. with the paths property)

       The (optional) fileName attribute specifies which file to take (relative to
       dependency-cruiser's current working directory). When not provided
       defaults to './tsconfig.json'.
     */
    tsConfig: {
      fileName: 'tsconfig.json'
    },

    /* Webpack configuration to use to get resolve options from.

       The (optional) fileName attribute specifies which file to take (relative
       to dependency-cruiser's current working directory). When not provided defaults
       to './webpack.conf.js'.

       The (optional) `env` and `arguments` attributes contain the parameters
       to be passed if your webpack config is a function and takes them (see
        webpack documentation for details)
     */
    // webpackConfig: {
    //  fileName: 'webpack.config.js',
    //  env: {},
    //  arguments: {}
    // },

    /* Babel config ('.babelrc', '.babelrc.json', '.babelrc.json5', ...) to use
      for compilation
     */
    // babelConfig: {
    //   fileName: '.babelrc',
    // },

    /* List of strings you have in use in addition to cjs/ es6 requires
       & imports to declare module dependencies. Use this e.g. if you've
       re-declared require, use a require-wrapper or use window.require as
       a hack.
    */
    // exoticRequireStrings: [],

    /* options to pass on to enhanced-resolve, the package dependency-cruiser
       uses to resolve module references to disk. The values below should be
       suitable for most situations

       If you use webpack: you can also set these in webpack.conf.js. The set
       there will override the ones specified here.
     */
    enhancedResolveOptions: {
      /* What to consider as an 'exports' field in package.jsons */
      exportsFields: ["exports"],
      /* List of conditions to check for in the exports field.
         Only works when the 'exportsFields' array is non-empty.
      */
      conditionNames: ["import", "require", "node", "default", "types"],
      /* The extensions, by default are the same as the ones dependency-cruiser
         can access (run `npx depcruise --info` to see which ones that are in
         _your_ environment). If that list is larger than you need you can pass
         the extensions you actually use (e.g. [".js", ".jsx"]). This can speed
         up module resolution, which is the most expensive step.
       */
      extensions: [".tsx", ".ts"],
      /* What to consider a 'main' field in package.json */
      mainFields: ["module", "main", "types", "typings"],
      /* A list of alias fields in package.jsons

         See [this specification](https://github.com/defunctzombie/package-browser-field-spec) and
         the webpack [resolve.alias](https://webpack.js.org/configuration/resolve/#resolvealiasfields)
         documentation.

         Defaults to an empty array (= don't use alias fields).
       */
      // aliasFields: ["browser"],
    },

    /* skipAnalysisNotInRules will make dependency-cruiser execute
       analysis strictly necessary for checking the rule set only.

       See https://github.com/sverweij/dependency-cruiser/blob/main/doc/options-reference.md#skipanalysisnotinrules
       for details
     */
    skipAnalysisNotInRules: true,

    reporterOptions: {
      dot: {
        /* pattern of modules that can be consolidated in the detailed
           graphical dependency graph. The default pattern in this configuration
           collapses everything in node_modules to one folder deep so you see
           the external modules, but their innards.
         */
        collapsePattern: 'node_modules/(?:@[^/]+/[^/]+|[^/]+)',

        /* Options to tweak the appearance of your graph.See
           https://github.com/sverweij/dependency-cruiser/blob/main/doc/options-reference.md#reporteroptions
           for details and some examples. If you don't specify a theme
           dependency-cruiser falls back to a built-in one.
        */
        // theme: {
        //   graph: {
        //     /* splines: "ortho" gives straight lines, but is slow on big graphs
        //        splines: "true" gives bezier curves (fast, not as nice as ortho)
        //    */
        //     splines: "true"
        //   },
        // }
      },
      archi: {
        /* pattern of modules that can be consolidated in the high level
           graphical dependency graph. If you use the high level graphical
           dependency graph reporter (`archi`) you probably want to tweak
           this collapsePattern to your situation.
        */
        collapsePattern: '^(?:packages|src|lib(s?)|app(s?)|bin|test(s?)|spec(s?))/[^/]+|node_modules/(?:@[^/]+/[^/]+|[^/]+)',

        /* Options to tweak the appearance of your graph. If you don't specify a
           theme for 'archi' dependency-cruiser will use the one specified in the
           dot section above and otherwise use the default one.
         */
        // theme: { },
      },
      "text": {
        "highlightFocused": true
      },
    }
  }
};
// Generated for NexusArchive - Architecture Defense System
// J2: Self-Check - Automated architecture validation
// Last updated: 2025-01-06 - Batch Upload Compliance (DA/T 94-2022)
