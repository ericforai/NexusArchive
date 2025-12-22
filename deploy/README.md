一旦我所属的文件夹有所变化，请更新我。
本目录包含部署、打包与运维脚本入口。
用于构建离线包、生成发布物与配置服务。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `build.sh` | 构建脚本 | 生成标准发布物/包 |
| `build_incremental.sh` | 构建脚本 | 执行增量构建流程 |
| `build_offline_package.sh` | 构建脚本 | 生成离线安装包 |
| `build_patch.sh` | 构建脚本 | 生成补丁包 |
| `build_security_patch.sh` | 构建脚本 | 生成安全补丁包 |
| `deploy.sh` | 部署脚本 | 执行部署流程入口 |
| `docker-compose.yml` | 配置入口 | 容器化部署编排 |
| `helm/` | 目录入口 | Helm Chart 与 K8s 部署配置 |
| `init_server.sh` | 运维脚本 | 初始化服务器环境 |
| `install_db.sh` | 运维脚本 | 安装/初始化数据库 |
| `monitoring/` | 目录入口 | 监控相关配置与脚本 |
| `nexusarchive-release.tar.gz` | 发布产物 | 预构建发布包（二进制） |
| `nexusarchive.service` | 服务配置 | systemd 服务单元 |
| `nginx.conf` | 服务配置 | 反向代理配置 |
| `offline/` | 目录入口 | 离线安装包结构与脚本 |
| `setup_demo_aip.sh` | 运维脚本 | 演示 AIP 初始化 |
| `setup_ssl.sh` | 运维脚本 | SSL 证书配置 |
| `tools/` | 目录入口 | 部署辅助工具集合 |
