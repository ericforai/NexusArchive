#!/bin/bash
# Docker问题预防检查脚本
# 用法: ./scripts/check-docker-health.sh

echo "=== Docker构建健康检查 ==="
echo ""

FAILED=0

# 检查1: .dockerignore存在性
echo "📋 检查1: .dockerignore配置"
if [ ! -f .dockerignore ]; then
    echo "❌ 缺少.dockerignore文件"
    FAILED=1
else
    echo "✅ .dockerignore存在"
    
    # 检查是否排除了关键文件
    REQUIRED_EXCLUDES=("*.log" "*.tar.gz")
    for pattern in "${REQUIRED_EXCLUDES[@]}"; do
        if grep -q "$pattern" .dockerignore; then
            echo "  ✅ 排除模式: $pattern"
        else
            echo "  ⚠️  建议添加排除模式: $pattern"
        fi
    done
fi
echo ""

# 检查2: 健康检查使用127.0.0.1而非localhost（排除CORS配置）
echo "🏥 检查2: 健康检查配置"
for compose_file in docker-compose*.yml; do
    if [ -f "$compose_file" ]; then
        # 只检查healthcheck区域中的localhost
        if grep -A 1 "healthcheck:" "$compose_file" | grep -q "localhost"; then
            echo "❌ $compose_file: 健康检查使用localhost（可能在IPv6环境失败）"
            echo "   建议: 使用 127.0.0.1 替代 localhost"
            FAILED=1
        else
            echo "✅ $compose_file: 健康检查配置正确"
        fi
    fi
done
echo ""

# 检查3: 大文件警告
echo "📦 检查3: 大文件检测"
LARGE_FILES=$(find . -maxdepth 1 -type f -size +50M 2>/dev/null | grep -v node_modules | grep -v ".git")
if [ -n "$LARGE_FILES" ]; then
    echo "⚠️  发现大文件（应添加到.dockerignore）:"
    echo "$LARGE_FILES" | while read file; do
        size=$(du -h "$file" | cut -f1)
        echo "   $size - $file"
    done
else
    echo "✅ 根目录无超大文件"
fi
echo ""

# 检查4: Docker基础镜像
echo "🐳 检查4: Docker基础镜像建议"
echo "💡 建议: 首次构建前预先拉取基础镜像"
echo "   docker pull maven:3.9-eclipse-temurin-17"
echo "   docker pull eclipse-temurin:17-jdk"
echo "   docker pull node:20-alpine"
echo ""

# 总结
if [ $FAILED -eq 0 ]; then
    echo "✅ 所有检查通过！"
    exit 0
else
    echo "❌ 发现问题需要修复"
    exit 1
fi
