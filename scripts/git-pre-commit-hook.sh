#!/bin/bash
# Git pre-commit hook - 自动检查Docker配置

echo "🔍 检查Docker配置..."

# 检查是否有新的大文件
LARGE_FILES=$(git diff --cached --name-only | xargs -I {} find {} -type f -size +10M 2>/dev/null)
if [ -n "$LARGE_FILES" ]; then
    echo "⚠️  警告：试图提交大文件（>10MB）:"
    echo "$LARGE_FILES"
    echo "请检查是否应该添加到.dockerignore"
    echo "如果确认要提交，使用: git commit --no-verify"
    exit 1
fi

# 检查docker-compose文件中的localhost
# 使用更精确的方法：检查暂存文件内容
for file in $(git diff --cached --name-only | grep docker-compose); do
    # 获取暂存文件内容，查找healthcheck区域内的localhost（排除注释）
    BAD_CONFIG=$(git show :"$file" | awk '
        /healthcheck:/ { in_healthcheck=1 }
        /^[[:space:]]*[^[:space:]#]/ { if (in_healthcheck && /localhost/) print FILENAME":"NR":"$0; in_healthcheck=0 }
        /^[[:space:]]*$/ { in_healthcheck=0 }
    ')
    
    if [ -n "$BAD_CONFIG" ]; then
        echo "❌ 错误：$file 中使用了localhost"
        echo "健康检查必须使用 127.0.0.1"
        echo "问题行："
        echo "$BAD_CONFIG"
        echo "如果确认要提交，使用: git commit --no-verify"
        exit 1
    fi
done

echo "✅ Docker配置检查通过"
