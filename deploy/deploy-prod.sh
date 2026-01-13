#!/bin/bash
# Input: 生产环境一键部署脚本
# Output: 部署并启动生产服务
# Pos: 服务器部署脚本
# 用法: ./deploy/deploy-prod.sh [domain]
# 示例: ./deploy/deploy-prod.sh archive.example.com

# 切换到脚本所在目录（确保相对路径正确）
cd "$(dirname "$0")/.."

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 默认参数
DOMAIN=${1:-archive.yourcompany.com}
INSTALL_DIR="/opt/nexusarchive"
BACKUP_DIR="/opt/nexusarchive/backups"
COMPOSE_FILES="-f docker-compose.infra.yml -f docker-compose.app.yml -f docker-compose.prod.yml"
ENV_FILE=".env.prod"

# 打印带颜色的消息
info() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# 打印欢迎信息
cat << 'EOF'
==========================================
   NexusArchive 生产环境部署
==========================================
EOF

info "域名: $DOMAIN"
info "安装目录: $INSTALL_DIR"
echo ""

# ========== 步骤 1: 环境检查 ==========
info "步骤 1/9: 环境检查..."

# 检查 Docker
if ! command -v docker &> /dev/null; then
    error "Docker 未安装，请先安装 Docker"
fi
info "✓ Docker 已安装: $(docker --version)"

# 检查 docker-compose
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    error "docker-compose 未安装，请先安装 docker-compose"
fi
info "✓ docker-compose 已就绪"

# 检查端口占用
if docker ps 2>/dev/null | grep -q "0.0.0.0:80\|0.0.0.0:443"; then
    warn "端口 80/443 已被占用，部署前请先停止冲突的服务"
fi

# ========== 步骤 2: 创建目录结构 ==========
info "步骤 2/9: 创建目录结构..."
sudo mkdir -p $INSTALL_DIR/{archives,data,logs,scripts,backups,nginx/ssl}
sudo chown -R $USER:$USER $INSTALL_DIR 2>/dev/null || true
info "✓ 目录结构创建完成"

# ========== 步骤 3: 生成安全密钥 ==========
info "步骤 3/9: 生成安全配置..."

if [ ! -f "$ENV_FILE" ]; then
    if [ -f ".env.prod.example" ]; then
        cp .env.prod.example $ENV_FILE
    else
        warn ".env.prod.example 不存在，使用默认模板"
        cat > $ENV_FILE << 'ENV_EOF'
# NexusArchive 生产环境配置
SPRING_PROFILES_ACTIVE=prod
TAG=latest

# 数据库配置
DB_HOST=nexus-db
DB_PORT=5432
DB_NAME=nexusarchive
DB_USER=postgres
DB_PASSWORD=CHANGE_ME

# Redis 配置
REDIS_HOST=nexus-redis
REDIS_PORT=6379

# 安全配置
SM4_KEY=CHANGE_ME
AUDIT_LOG_HMAC_KEY=CHANGE_ME
JWT_SECRET=CHANGE_ME

# CORS
APP_SECURITY_CORS_ALLOWED_ORIGINS=https://CHANGE_ME

# 其他配置
VIRUS_SCAN_TYPE=mock
TIMESTAMP_FALLBACK=true
SCHEMA_VALIDATION_ENABLED=false
APP_DEBUG_ENABLED=false
ENV_EOF
    fi

    # 生成随机密钥
    info "生成随机安全密钥..."
    DB_PASSWORD=$(openssl rand -base64 16 | tr -d '/+=')
    SM4_KEY=$(openssl rand -hex 16)
    HMAC_KEY=$(openssl rand -hex 32)
    JWT_SECRET=$(openssl rand -base64 32)

    sed -i.bak "s/your_secure_database_password_here/$DB_PASSWORD/" $ENV_FILE 2>/dev/null || \
        sed -i '' "s/your_secure_database_password_here/$DB_PASSWORD/" $ENV_FILE
    sed -i.bak "s/your_32_char_hex_key_here/$SM4_KEY/" $ENV_FILE 2>/dev/null || \
        sed -i '' "s/your_32_char_hex_key_here/$SM4_KEY/" $ENV_FILE
    sed -i.bak "s/your_jwt_secret_key_here/$JWT_SECRET/" $ENV_FILE 2>/dev/null || \
        sed -i '' "s/your_jwt_secret_key_here/$JWT_SECRET/" $ENV_FILE
    sed -i.bak "s/your_audit_log_hmac_key_here/$HMAC_KEY/" $ENV_FILE 2>/dev/null || \
        sed -i '' "s/your_audit_log_hmac_key_here/$HMAC_KEY/" $ENV_FILE
    sed -i.bak "s|https://your-domain.com|https://$DOMAIN|" $ENV_FILE 2>/dev/null || \
        sed -i '' "s|https://your-domain.com|https://$DOMAIN|" $ENV_FILE
    sed -i.bak "s/CHANGE_ME/$DB_PASSWORD/" $ENV_FILE 2>/dev/null || \
        sed -i '' "s/DB_PASSWORD=.*/DB_PASSWORD=$DB_PASSWORD/" $ENV_FILE

    rm -f ${ENV_FILE}.bak

    warn "已生成 $ENV_FILE，请检查并补充配置（如 YonSuite 凭证）"
else
    info "✓ $ENV_FILE 已存在，跳过生成"
fi

# ========== 步骤 4: 配置 Nginx ==========
info "步骤 4/9: 配置 Nginx..."

if [ -f "nginx/nginx.prod.template" ]; then
    sed "s/{{DOMAIN}}/$DOMAIN/g" nginx/nginx.prod.template > nginx/nginx.prod.conf
    info "✓ Nginx 配置已生成: nginx/nginx.prod.conf"
else
    warn "nginx.prod.template 不存在，使用默认配置"
fi

# ========== 步骤 5: 构建或拉取镜像 ==========
info "步骤 5/9: 准备 Docker 镜像..."

# 检查是否需要本地构建
if [ -f "nexusarchive-java/Dockerfile" ]; then
    info "构建后端镜像..."
    docker build -t nexusarchive-backend:${TAG:-latest} -f nexusarchive-java/Dockerfile nexusarchive-java
else
    info "后端 Dockerfile 不存在，跳过构建"
fi

if [ -f "Dockerfile.frontend.prod" ]; then
    info "构建前端镜像..."
    docker build -t nexusarchive-web:${TAG:-latest} -f Dockerfile.frontend.prod .
else
    info "前端 Dockerfile 不存在，跳过构建"
fi

# ========== 步骤 6: 启动服务 ==========
info "步骤 6/9: 启动服务..."

# 停止旧服务（如果存在）
if docker-compose $COMPOSE_FILES ps 2>/dev/null | grep -q "Up"; then
    warn "检测到运行中的服务，先停止..."
    docker-compose $COMPOSE_FILES down
fi

# 启动新服务
docker-compose $COMPOSE_FILES --env-file $ENV_FILE up -d

info "✓ 服务启动中..."

# ========== 步骤 7: 等待健康检查 ==========
info "步骤 7/9: 等待服务就绪..."

MAX_WAIT=60
WAIT_TIME=0
while [ $WAIT_TIME -lt $MAX_WAIT ]; do
    if docker-compose $COMPOSE_FILES ps | grep -q "healthy\|Up"; then
        info "✓ 服务已启动"
        break
    fi
    sleep 5
    WAIT_TIME=$((WAIT_TIME + 5))
    echo -n "."
done
echo ""

# ========== 步骤 8: 显示状态 ==========
info "步骤 8/9: 服务状态"
docker-compose $COMPOSE_FILES ps

# ========== 步骤 9: 完成 ==========
echo ""
cat << EOF
==========================================
   ✅ 部署完成!
==========================================

📍 访问地址: http://$DOMAIN
📝 API 文档: http://$DOMAIN/api/swagger-ui.html

🔧 管理命令:
   查看日志: docker-compose $COMPOSE_FILES logs -f
   停止服务: docker-compose $COMPOSE_FILES down
   重启服务: docker-compose $COMPOSE_FILES restart

⚠️  配置 HTTPS (推荐):
   sudo apt-get install certbot python3-certbot-nginx
   sudo certbot certonly --nginx -d $DOMAIN
   docker restart nexus-frontend

📋 下一步:
   1. 访问系统并修改默认密码
   2. 配置定期备份: crontab -e
      0 2 * * * $INSTALL_DIR/scripts/backup.sh

==========================================
EOF
