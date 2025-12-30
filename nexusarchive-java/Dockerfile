# ==============================================================================
# NexusArchive 后端 Dockerfile - 多阶段构建
# 
# 优点：
# - 本地无需安装 Maven/JDK，只需 Docker
# - 编译环境 100% 一致
# - 最终镜像更小（只含 JRE）
# ==============================================================================

# ==================== Stage 1: 编译阶段 ====================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 先复制 pom.xml 并下载依赖（利用 Docker 缓存）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并编译
COPY src/ src/
RUN mvn package -DskipTests -B

# ==================== Stage 2: 运行阶段 ====================
FROM eclipse-temurin:17-jdk

WORKDIR /app

# 预创建目录
RUN mkdir -p /app/keystore /app/data /app/logs

# 从编译阶段复制 JAR
COPY --from=builder /build/target/nexusarchive-backend-*.jar app.jar

# 复制中文字体（解决 PDF 乱码问题）
RUN mkdir -p /usr/share/fonts/truetype/noto
COPY src/main/resources/fonts/NotoSansSC-Regular.ttf /usr/share/fonts/truetype/noto/

# 复制 keystore（如果存在，否则依赖 volume 挂载）
COPY keystore/* /app/keystore/

# 环境变量默认值
ENV DB_HOST=nexus-db \
    DB_PORT=5432 \
    DB_NAME=nexusarchive \
    DB_USER=postgres \
    DB_PASS=postgres \
    REDIS_HOST=nexus-redis

EXPOSE 19090

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:19090/api/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
