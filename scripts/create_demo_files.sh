#!/bin/bash
# Input: Shell、mkdir
# Output: 演示数据导入
# Pos: 运维脚本
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。


# Config matches the SQL data
ARCHIVE_ROOT="/tmp/archives"
CODE="COMP001-2023-10Y-FIN-AC01-V0088"
DIR="$ARCHIVE_ROOT/COMP001/2023/10Y/AC01/$CODE/content"

echo "Creating physical files for demo AIP: $CODE"

# Create directory
mkdir -p "$DIR"

# 1. Create Main Voucher (OFD)
echo "Dummy OFD Content" > "$DIR/voucher_v0088.ofd"

# 2. Create Attachment (Contract)
echo "Dummy Contract PDF Content" > "$DIR/contract_2023_001.pdf"

# 3. Create Attachment (Bank Slip)
echo "Dummy Bank Slip Image Content" > "$DIR/bank_slip_001.jpg"

echo "Physical files created at $DIR"