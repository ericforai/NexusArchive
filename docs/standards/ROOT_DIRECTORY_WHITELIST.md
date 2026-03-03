# Root Directory Whitelist（根目录白名单）

目的：约束项目根目录只保留“入口级文件/目录”，其余内容必须进入对应子目录，避免再次堆积杂项。

## 白名单（允许长期存在于根目录）

### 1) 工程入口与核心配置

- `README.md`
- `AGENTS.md`
- `CLAUDE.md`
- `package.json`
- `package-lock.json`
- `tsconfig.json`
- `vite.config.ts`
- `vite.config.ts.minimal`
- `playwright.config.ts`
- `.gitignore`
- `.dockerignore`
- `.eslintrc.cjs`
- `.eslintrc.complexity.cjs`
- `eslint.config.cjs`
- `.dependency-cruiser.cjs`
- `.env.example`
- `.env.template`
- `.env.prod.example`
- `docker-compose.app.yml`
- `docker-compose.infra.yml`
- `docker-compose.prod.yml`
- `Dockerfile.frontend`
- `Dockerfile.frontend.prod`

### 2) 顶层业务目录

- `src/`
- `public/`
- `tests/`
- `scripts/`
- `docs/`
- `deploy/`
- `tools/`
- `db/`
- `data/`
- `nexusarchive-java/`
- `nexus-core/`
- `conductor/`
- `nginx/`

### 3) 运行期保留项（可临时存在）

- `backend.log`
- `frontend.log`
- `.backend.pid`
- `.frontend.pid`

说明：这四项是当前开发脚本默认输出位置，不建议移动；历史文件应及时归档。

## 非白名单文件去向（强制）

- 诊断/一次性报告：`docs/archive/root-artifacts/diagnostics/`
- 历史测试结果：`docs/archive/root-artifacts/test-reports/`
- 示例输入文件：`docs/archive/root-artifacts/samples/`
- 历史日志：`logs/root-legacy/`
- 根目录散落 SQL：`backups/root-sql/`
- 临时脚本：`scripts/temp/`

## 禁止事项

- 禁止在根目录新增 `*.sql`、`*.txt`、`*.json` 作为一次性产物。
- 禁止在根目录长期保留非 `backend.log/frontend.log` 的日志文件。
- 禁止将大体积临时文件（导出包、截图、测试中间件）直接放根目录。

## 提交前自检

```bash
# 1) 检查根目录是否出现非白名单的散文件
find . -maxdepth 1 -type f | sed 's#^./##' | sort

# 2) 检查根目录是否有新增 SQL/日志
find . -maxdepth 1 -type f \( -name '*.sql' -o -name '*.log' \) | sed 's#^./##' | sort
```

## 维护要求

- 新增根目录文件前，先判断是否属于白名单；不属于则放到对应子目录。
- 如确需新增白名单项，必须先更新本文件，再提交代码。
