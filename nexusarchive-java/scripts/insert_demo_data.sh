#!/bin/bash
# 档案审批与开放鉴定演示数据插入脚本
# Demo Data Insertion Script for Archive Approval and Open Appraisal

API_BASE="http://localhost:8080/api"

echo "开始插入档案审批演示数据..."
echo "================================"

# 待审批记录 1
curl -X POST "${API_BASE}/archive-approval/create" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "AA-2024-001",
    "archiveId": "ARCH-2024-001",
    "archiveCode": "QZ-2024-KJ-001",
    "archiveTitle": "2024年1月记账凭证",
    "applicantId": "user001",
    "applicantName": "张三",
    "applicationReason": "完成四性检测，申请正式归档",
    "status": "PENDING"
  }'
echo ""

# 待审批记录 2
curl -X POST "${API_BASE}/archive-approval/create" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "AA-2024-002",
    "archiveId": "ARCH-2024-002",
    "archiveCode": "QZ-2024-KJ-002",
    "archiveTitle": "2024年2月记账凭证",
    "applicantId": "user002",
    "applicantName": "李四",
    "applicationReason": "凭证已完成关联，申请归档",
    "status": "PENDING"
  }'
echo ""

# 待审批记录 3
curl -X POST "${API_BASE}/archive-approval/create" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "AA-2024-003",
    "archiveId": "ARCH-2024-003",
    "archiveCode": "QZ-2024-BB-001",
    "archiveTitle": "2024年第一季度财务报告",
    "applicantId": "user003",
    "applicantName": "王五",
    "applicationReason": "季度报告已审核，申请归档",
    "status": "PENDING"
  }'
echo ""

# 待审批记录 4
curl -X POST "${API_BASE}/archive-approval/create" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "AA-2024-004",
    "archiveId": "ARCH-2024-004",
    "archiveCode": "QZ-2024-KJ-003",
    "archiveTitle": "2024年3月记账凭证",
    "applicantId": "user004",
    "applicantName": "赵六",
    "applicationReason": "月度凭证整理完成，申请归档",
    "status": "PENDING"
  }'
echo ""

# 待审批记录 5
curl -X POST "${API_BASE}/archive-approval/create" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "AA-2024-005",
    "archiveId": "ARCH-2024-005",
    "archiveCode": "QZ-2024-ZB-001",
    "archiveTitle": "2024年总账（1-3月）",
    "applicantId": "user002",
    "applicantName": "李四",
    "applicationReason": "季度总账，申请归档",
    "status": "PENDING"
  }'
echo ""

echo ""
echo "开始插入开放鉴定演示数据..."
echo "================================"

# 待鉴定记录 1
curl -X POST "${API_BASE}/open-appraisal/create" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "OA-2014-001",
    "archiveId": "ARCH-2014-001",
    "archiveCode": "QZ-2014-KJ-001",
    "archiveTitle": "2014年1月记账凭证",
    "retentionPeriod": "10Y",
    "currentSecurityLevel": "INTERNAL",
    "status": "PENDING"
  }'
echo ""

# 待鉴定记录 2
curl -X POST "${API_BASE}/open-appraisal/create" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "OA-2014-002",
    "archiveId": "ARCH-2014-002",
    "archiveCode": "QZ-2014-KJ-002",
    "archiveTitle": "2014年2月记账凭证",
    "retentionPeriod": "10Y",
    "currentSecurityLevel": "INTERNAL",
    "status": "PENDING"
  }'
echo ""

# 待鉴定记录 3
curl -X POST "${API_BASE}/open-appraisal/create" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "OA-2014-003",
    "archiveId": "ARCH-2014-003",
    "archiveCode": "QZ-2014-BB-001",
    "archiveTitle": "2014年第一季度财务报告",
    "retentionPeriod": "10Y",
    "currentSecurityLevel": "INTERNAL",
    "status": "PENDING"
  }'
echo ""

# 待鉴定记录 4
curl -X POST "${API_BASE}/open-appraisal/create" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "OA-2013-004",
    "archiveId": "ARCH-2013-004",
    "archiveCode": "QZ-2013-HT-001",
    "archiveTitle": "2013年设备采购合同",
    "retentionPeriod": "10Y",
    "currentSecurityLevel": "INTERNAL",
    "status": "PENDING"
  }'
echo ""

# 待鉴定记录 5
curl -X POST "${API_BASE}/open-appraisal/create" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "OA-2013-005",
    "archiveId": "ARCH-2013-005",
    "archiveCode": "QZ-2013-KJ-012",
    "archiveTitle": "2013年12月记账凭证",
    "retentionPeriod": "10Y",
    "currentSecurityLevel": "INTERNAL",
    "status": "PENDING"
  }'
echo ""

# 待鉴定记录 6
curl -X POST "${API_BASE}/open-appraisal/create" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "OA-2013-006",
    "archiveId": "ARCH-2013-006",
    "archiveCode": "QZ-2013-BB-004",
    "archiveTitle": "2013年度财务决算报告",
    "retentionPeriod": "10Y",
    "currentSecurityLevel": "SECRET",
    "status": "PENDING"
  }'
echo ""

echo ""
echo "✅ 演示数据插入完成！"
echo "================================"
echo "档案审批：5条待审批记录"
echo "开放鉴定：6条待鉴定记录"
echo ""
echo "请刷新浏览器查看效果"
