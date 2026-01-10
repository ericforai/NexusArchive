#!/bin/bash
# 正确的脚本示例 - 使用 set -a

set -a
source .env.local
set +a

echo "SERVER_PORT=$SERVER_PORT"
