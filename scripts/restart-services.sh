#!/bin/bash
# Input: N/A
# Output: 调用 dev-start.sh
# Pos: 运维脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

# Determine the directory where this script is located
DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Redirection: Calling dev-start.sh..."
exec "$DIR/dev-start.sh" "$@"