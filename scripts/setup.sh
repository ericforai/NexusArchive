#!/bin/bash
# 开发环境自动设置脚本
# 用法: ./scripts/setup.sh

set -e

echo "=== NexusArchive 开发环境设置 ==="
echo ""

# 安装Git hooks
echo "📋 [1/3] 安装Git hooks..."
if [ -f scripts/git-pre-commit-hook.sh ]; then
    cp scripts/git-pre-commit-hook.sh .git/hooks/pre-commit
    chmod +x .git/hooks/pre-commit
    echo "✅ Git hook已安装"
else
    echo "⚠️  警告: scripts/git-pre-commit-hook.sh 不存在"
fi
echo ""

# 验证Docker配置
echo "🔍 [2/3] 验证Docker配置..."
if [ -f scripts/check-docker-health.sh ]; then
    chmod +x scripts/check-docker-health.sh
    ./scripts/check-docker-health.sh
else
    echo "⚠️  警告: scripts/check-docker-health.sh 不存在"
fi
echo ""

# 预拉取Docker镜像（可选）
echo "🐳 [3/3] 预拉取Docker镜像（可选）..."
echo "提示: 这将节省首次构建时间，但需要下载~2GB数据"
read -p "是否现在下载？(y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "正在拉取镜像（可能需要10-30分钟）..."
    docker pull maven:3.9-eclipse-temurin-17 &
    docker pull eclipse-temurin:17-jdk &
    docker pull node:20-alpine &
    docker pull postgres:14-alpine &
    docker pull redis:7-alpine &
    wait
    echo "✅ 镜像拉取完成"
else
    echo "⏭ 跳过镜像预拉取"
    echo "   首次构建时Docker会自动下载"
fi
echo ""

echo "=== ✅ 开发环境设置完成 ==="
echo ""
echo "下一步操作："
echo "  1. 启动开发环境:"
echo "     docker-compose -f docker-compose.dev.yml up -d"
echo ""
echo "  2. 查看服务状态:"
echo "     docker ps"
echo ""
echo "  3. 查看日志:"
echo "     docker-compose -f docker-compose.dev.yml logs -f"
echo ""
echo "📚 完整文档: docs/DOCKER_BUILD_GUIDE.md"
