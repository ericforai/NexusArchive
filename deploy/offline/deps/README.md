一旦我所属的文件夹有所变化，请更新我。
本目录存放离线依赖说明。
用于记录第三方依赖。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |

# 离线依赖

如需在完全断网的环境部署，请将以下文件放入此目录:

## JDK (必需，如服务器无 Java)

从以下地址下载 OpenJDK 17:
- https://adoptium.net/zh-CN/temurin/releases/?version=17

文件命名规则:
- x86_64: `jdk-17_linux-x86_64.tar.gz`
- ARM64:  `jdk-17_linux-aarch64.tar.gz`

## 数据库驱动 (可选)

如使用信创数据库:
- 达梦: `DmJdbcDriver18.jar` (已内置于应用)
- 金仓: `kingbase8-jdbc.jar` (已内置于应用)

## Nginx (可选)

如服务器无 Nginx，可放入:
- nginx-1.24.0.tar.gz (需手动编译安装)

或使用各发行版的离线 RPM/DEB 包。
