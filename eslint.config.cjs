const { FlatCompat } = require('@eslint/eslintrc');
const js = require('@eslint/js');

const compat = new FlatCompat({
    baseDirectory: __dirname,
    resolvePluginsRelativeTo: __dirname,
    recommendedConfig: js.configs.recommended,
    allConfig: js.configs.all,
});

// 复杂度检查模式：只使用复杂度配置，避免插件冲突
if (process.env.ESLINT_COMPLEXITY_MODE === '1') {
    module.exports = require('./.eslintrc.complexity.cjs');
} else {
    module.exports = [...compat.config(require('./.eslintrc.cjs'))];
}
