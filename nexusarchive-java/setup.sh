#!/bin/bash

echo "========================================="
echo "NexusArchive Java Backend Setup"
echo "========================================="

# 检查Java版本
echo "检查Java版本..."
java -version 2>&1 | grep "version" | awk '{print $3}' | sed 's/"//g'

if [ $? -ne 0 ]; then
    echo "错误: 未安装Java，请先安装Java 17+"
    exit 1
fi

# 检查Maven
echo "检查Maven..."
mvn -version | grep "Apache Maven"

if [ $? -ne 0 ]; then
    echo "错误: 未安装Maven，请先安装Maven 3.8+"
    exit 1
fi

# 检查PostgreSQL
echo "检查PostgreSQL..."
psql --version

if [ $? -ne 0 ]; then
    echo "警告: 未安装PostgreSQL"
    echo "请运行: brew install postgresql@14"
fi

# 创建数据库
echo "创建数据库..."
createdb nexusarchive 2>/dev/null || echo "数据库已存在"

# 初始化Schema
echo "初始化数据库Schema..."
psql -U postgres -d nexusarchive -f src/main/resources/sql/schema-postgresql.sql

if [ $? -eq 0 ]; then
    echo "✅ 数据库初始化成功"
else
    echo "❌ 数据库初始化失败"
    exit 1
fi

# 编译项目
echo "编译项目..."
mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ 项目编译成功"
else
    echo "❌ 项目编译失败"
    exit 1
fi

echo "========================================="
echo "✅ 设置完成!"
echo "运行项目: mvn spring-boot:run"
echo "访问: http://localhost:8080/api"
echo "========================================="
