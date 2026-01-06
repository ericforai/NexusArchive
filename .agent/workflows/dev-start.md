# 开发环境启动与运维工作流

## 1. 环境同步使用方法

本项目支持"公司至家里"的开发数据无缝同步，解决私有化部署环境下的数据一致性问题。

- **在公司/旧设备**：`npm run db:dump` (导出当前数据库快照到 `db/seed-data.sql`)
- **在家里/新设备**：`npm run db:load` (从 `db/seed-data.sql` 恢复快照)

---

## 2. 核心文件结构

| 文件 | 用途 |
|------|------|
| `docker-compose.infra.yml` | 基础设施（PostgreSQL + Redis），本地开发使用 |
| `docker-compose.app.yml` | 全栈服务（后端 + 前端 + DB），服务器部署使用 |
| `scripts/dev.sh` | 本地开发一键启动脚本 |
| `scripts/dev-stop.sh` | 本地开发一键停止脚本 |

---

## 3. 常用开发命令

// turbo
```bash
npm run dev        # 启动环境（Docker 跑 DB，本地跑前后端）
```

```bash
npm run dev:stop   # 停止本地开发环境进程及容器
```

```bash
npm run db:dump    # 导出本地数据库数据（离开公司前执行）
```

```bash
npm run db:load    # 导入数据快照（回到家后在新环境执行）
```

```bash
npm run db:reset   # 重置数据库（删除所有数据并重新初始化）
```

```bash
npm run deploy     # 部署到预发/生产服务器
```

---

## 4. 当前服务状态

| 服务 | 运行模式 | 端口 | 访问地址 |
|------|---------|------|---------|
| **PostgreSQL** | Docker | 54321 | `localhost:54321` |
| **Redis** | Docker | 16379 | `localhost:16379` |
| **后端 API** | 本地 (Maven) | 19090 | `http://localhost:19090/api` |
| **前端 UI** | 本地 (Vite) | 15175 | `http://localhost:15175` |

---

## 常见操作说明

### 首次克隆项目
1. 确保已安装 Docker, JDK, Node.js, Maven
2. 执行 `npm run dev`
3. 脚本会自动创建 `.env.local` 并初始化数据库

### 端口占用清理
如果遇到端口被占用的报错，运行 `npm run dev:stop` 会尝试自动强制释放端口。
手动清理：
```bash
lsof -ti :19090,15175 | xargs kill -9
```

### 查看日志
后端日志持续输出到根目录下的 `backend.log`：
```bash
tail -f backend.log
```
