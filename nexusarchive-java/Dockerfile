# ==============================================================================
# NexusArchive 后端 Dockerfile - 多阶段构建
#
# 优化目标：
# - 适用于 4GB 内存服务器
# - 最终镜像 < 200MB
# - 本地构建，服务器免编译
# ==============================================================================

# ==================== Stage 1: 编译阶段 ====================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# 复制 pom.xml 并下载依赖（利用 Docker 缓存层）
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# 复制源码并编译
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests -B && \
    # 清理不必要的文件以减小中间层大小
    rm -rf /root/.m2/repository/.cache

# ==================== Stage 2: 运行阶段（JRE + Alpine） ====================
FROM eclipse-temurin:17-jre-alpine

# 安装运行时依赖（curl 用于健康检查，字体用于 PDF）
RUN apk add --no-cache \
    curl \
    ttf-dejavu \
    fontconfig && \
    # 预创建目录
    mkdir -p /app/keystore /app/data /app/logs /usr/share/fonts/truetype/noto

WORKDIR /app

# 从编译阶段复制 JAR
COPY --from=builder /build/target/nexusarchive-backend-*.jar app.jar

# 复制中文字体（解决 PDF 中文乱码）
COPY --from=builder /build/src/main/resources/fonts/NotoSansSC-Regular.ttf /usr/share/fonts/truetype/noto/
RUN fc-cache -f

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

# 健康检查（轻量级）
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:19090/api/health || exit 1

# JVM 内存优化（适配 4GB 服务器，容器限制 2GB）
ENV JAVA_OPTS="-Xms256m -Xmx768m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
