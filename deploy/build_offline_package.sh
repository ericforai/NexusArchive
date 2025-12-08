#!/bin/bash
#====================================================================
# NexusArchive 离线安装包构建脚本
# 用于在开发环境构建可分发的离线安装包
#====================================================================
set -e

# 颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# 配置
VERSION="2.0.0"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/deploy/offline"
PACKAGE_NAME="nexusarchive-offline-installer-${VERSION}"

log_info() { echo -e "${GREEN}[BUILD]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

#====================================================================
# 构建步骤
#====================================================================

clean_output() {
    log_info "清理输出目录..."
    rm -rf "${OUTPUT_DIR}/bin"
    rm -rf "${OUTPUT_DIR}/frontend"
    rm -rf "${OUTPUT_DIR}/deps"
    rm -f "${OUTPUT_DIR}/${PACKAGE_NAME}.tar.gz"
    mkdir -p "${OUTPUT_DIR}"/{bin,frontend,deps,config,db/migration,docs}
}

build_backend() {
    log_info "构建后端 JAR..."
    cd "${PROJECT_ROOT}/nexusarchive-java"
    
    mvn clean package -DskipTests -q
    
    local jar_file="target/nexusarchive-backend-${VERSION}.jar"
    if [[ -f "$jar_file" ]]; then
        cp "$jar_file" "${OUTPUT_DIR}/bin/"
        log_info "后端构建完成: $(du -h "$jar_file" | awk '{print $1}')"
    else
        log_error "后端构建失败，未找到 JAR 文件"
        exit 1
    fi
}

build_frontend() {
    log_info "构建前端资源..."
    cd "${PROJECT_ROOT}"
    
    # 检查 node_modules
    if [[ ! -d "node_modules" ]]; then
        log_info "安装前端依赖..."
        npm install --silent
    fi
    
    npm run build --silent
    
    if [[ -d "dist" ]]; then
        cp -r dist/* "${OUTPUT_DIR}/frontend/"
        log_info "前端构建完成: $(du -sh dist | awk '{print $1}')"
    else
        log_error "前端构建失败，未找到 dist 目录"
        exit 1
    fi
}

copy_configs() {
    log_info "复制配置文件..."
    
    # Systemd 服务文件
    cp "${PROJECT_ROOT}/deploy/nexusarchive.service" "${OUTPUT_DIR}/config/"
    
    # Nginx 配置
    cp "${PROJECT_ROOT}/deploy/nginx.conf" "${OUTPUT_DIR}/config/"
    
    # 数据库迁移脚本
    cp "${PROJECT_ROOT}/nexusarchive-java/src/main/resources/db/migration/"*.sql "${OUTPUT_DIR}/db/migration/"
    
    log_info "配置文件复制完成"
}

create_docs() {
    log_info "生成安装文档..."
    
    cat > "${OUTPUT_DIR}/docs/安装手册.md" << 'EOF'
# NexusArchive 离线安装手册

## 系统要求

### 硬件
- CPU: 2核+ (推荐4核)
- 内存: 4GB+ (推荐8GB)
- 磁盘: 50GB+ (根据档案量调整)

### 软件
- 操作系统: CentOS 7/8, Ubuntu 18.04/20.04/22.04, 麒麟 V10, 统信 UOS
- Java: OpenJDK 17+ (可使用离线包中提供的 JDK)
- 数据库: PostgreSQL 12+ / 达梦 DM8 / 人大金仓 Kingbase V8

## 安装步骤

### 1. 解压安装包
```bash
tar -xzf nexusarchive-offline-installer-2.0.0.tar.gz
cd nexusarchive-offline-installer-2.0.0
```

### 2. 运行安装脚本
```bash
sudo ./install.sh
```

### 3. 按照配置向导完成设置
- 选择安装目录
- 配置数据库连接
- 设置服务端口

### 4. 验证安装
```bash
systemctl status nexusarchive
curl http://localhost:8080/api/health
```

## 配置文件模式 (无交互)

如需批量部署，可预先配置 `install.conf`:

```bash
cp install.conf.template install.conf
# 编辑 install.conf 填写配置
vim install.conf
# 运行安装
sudo ./install.sh
```

## 常见问题

### Q: Java 版本不对怎么办?
A: 将 JDK 17 的 tar.gz 放入 `deps/` 目录，安装脚本会自动使用。

### Q: 数据库连接失败?
A: 检查数据库服务是否启动，用户权限是否正确，防火墙是否放行端口。

### Q: 如何升级?
A: 使用 `upgrade.sh` 脚本进行升级，会自动备份原有数据。

## 技术支持

如有问题，请联系技术支持团队。
EOF
    
    log_info "文档生成完成"
}

create_uninstall_script() {
    log_info "生成卸载脚本..."
    
    cat > "${OUTPUT_DIR}/uninstall.sh" << 'EOF'
#!/bin/bash
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}警告: 此操作将完全卸载 NexusArchive${NC}"
echo "包括: 应用程序、配置文件"
echo "不包括: 数据库数据、归档文件 (需手动删除)"
echo ""
read -rp "确定要继续吗? [y/N]: " confirm

if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
    echo "已取消"
    exit 0
fi

echo -e "${GREEN}[1/4]${NC} 停止服务..."
systemctl stop nexusarchive 2>/dev/null || true
systemctl disable nexusarchive 2>/dev/null || true

echo -e "${GREEN}[2/4]${NC} 删除 Systemd 服务..."
rm -f /etc/systemd/system/nexusarchive.service
systemctl daemon-reload

echo -e "${GREEN}[3/4]${NC} 删除 Nginx 配置..."
rm -f /etc/nginx/sites-enabled/nexusarchive.conf 2>/dev/null || true
rm -f /etc/nginx/sites-available/nexusarchive.conf 2>/dev/null || true
rm -f /etc/nginx/conf.d/nexusarchive.conf 2>/dev/null || true
systemctl reload nginx 2>/dev/null || true

echo -e "${GREEN}[4/4]${NC} 删除应用目录..."
rm -rf /opt/nexusarchive

echo ""
echo -e "${GREEN}卸载完成${NC}"
echo ""
echo "以下内容需要手动清理:"
echo "  - 数据库: DROP DATABASE nexusarchive;"
echo "  - 归档文件: rm -rf /opt/nexusarchive/data"
echo "  - 日志: rm -rf /var/log/nexusarchive"
echo "  - 用户: userdel nexus"
EOF
    
    chmod +x "${OUTPUT_DIR}/uninstall.sh"
    log_info "卸载脚本生成完成"
}

create_upgrade_script() {
    log_info "生成升级脚本..."
    
    cat > "${OUTPUT_DIR}/upgrade.sh" << 'EOF'
#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION="2.0.0"
INSTALL_DIR="/opt/nexusarchive"
BACKUP_DIR="/opt/nexusarchive_backup_$(date +%Y%m%d_%H%M%S)"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "╔══════════════════════════════════════════════════════════╗"
echo "║     NexusArchive 升级程序 v${VERSION}                        ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

# 检查现有安装
if [[ ! -d "$INSTALL_DIR" ]]; then
    echo -e "${YELLOW}未检测到现有安装，请使用 install.sh 进行全新安装${NC}"
    exit 1
fi

echo -e "${GREEN}[1/5]${NC} 备份当前版本..."
mkdir -p "$BACKUP_DIR"
cp "$INSTALL_DIR/app.jar" "$BACKUP_DIR/" 2>/dev/null || true
cp -r "$INSTALL_DIR/frontend" "$BACKUP_DIR/" 2>/dev/null || true
cp "$INSTALL_DIR/.env" "$BACKUP_DIR/" 2>/dev/null || true
echo "备份位置: $BACKUP_DIR"

echo -e "${GREEN}[2/5]${NC} 停止服务..."
systemctl stop nexusarchive

echo -e "${GREEN}[3/5]${NC} 更新后端..."
cp "${SCRIPT_DIR}/bin/nexusarchive-backend-${VERSION}.jar" "$INSTALL_DIR/app.jar"

echo -e "${GREEN}[4/5]${NC} 更新前端..."
rm -rf "$INSTALL_DIR/frontend"
cp -r "${SCRIPT_DIR}/frontend" "$INSTALL_DIR/"

echo -e "${GREEN}[5/5]${NC} 启动服务..."
systemctl start nexusarchive

echo ""
echo -e "${GREEN}升级完成!${NC}"
echo "如需回滚，请执行:"
echo "  systemctl stop nexusarchive"
echo "  cp $BACKUP_DIR/app.jar $INSTALL_DIR/"
echo "  cp -r $BACKUP_DIR/frontend $INSTALL_DIR/"
echo "  systemctl start nexusarchive"
EOF
    
    chmod +x "${OUTPUT_DIR}/upgrade.sh"
    log_info "升级脚本生成完成"
}

create_deps_readme() {
    log_info "生成依赖说明..."
    
    cat > "${OUTPUT_DIR}/deps/README.md" << 'EOF'
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
EOF
    
    log_info "依赖说明生成完成"
}

package_all() {
    log_info "打包离线安装包..."
    
    cd "${OUTPUT_DIR}"
    
    # 确保脚本可执行
    chmod +x install.sh uninstall.sh upgrade.sh
    
    # 创建最终包
    cd ..
    tar -czf "${PACKAGE_NAME}.tar.gz" -C "${OUTPUT_DIR}" .
    mv "${PACKAGE_NAME}.tar.gz" "${OUTPUT_DIR}/"
    
    local package_size=$(du -h "${OUTPUT_DIR}/${PACKAGE_NAME}.tar.gz" | awk '{print $1}')
    
    echo ""
    echo "╔══════════════════════════════════════════════════════════╗"
    echo "║     ✅ 离线安装包构建完成                                ║"
    echo "╠══════════════════════════════════════════════════════════╣"
    echo "║                                                          ║"
    echo "║     输出文件: ${OUTPUT_DIR}/${PACKAGE_NAME}.tar.gz"
    echo "║     文件大小: ${package_size}                                      "
    echo "║                                                          ║"
    echo "║     分发说明:                                            ║"
    echo "║     1. 将 tar.gz 复制到目标服务器                        ║"
    echo "║     2. 解压: tar -xzf ${PACKAGE_NAME}.tar.gz             ║"
    echo "║     3. 运行: sudo ./install.sh                           ║"
    echo "║                                                          ║"
    echo "╚══════════════════════════════════════════════════════════╝"
    echo ""
}

#====================================================================
# 主流程
#====================================================================

main() {
    echo ""
    echo "╔══════════════════════════════════════════════════════════╗"
    echo "║     NexusArchive 离线安装包构建工具                      ║"
    echo "║     版本: ${VERSION}                                         ║"
    echo "╚══════════════════════════════════════════════════════════╝"
    echo ""
    
    clean_output
    build_backend
    build_frontend
    copy_configs
    create_docs
    create_uninstall_script
    create_upgrade_script
    create_deps_readme
    package_all
}

main "$@"
