# ==============================================================================
# NexusArchive 后端 Dockerfile - 简化构建（网络适配）
# ==============================================================================

# ==================== Stage 1: 编译阶段 ====================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 复制源码并编译（跳过依赖预下载）
COPY pom.xml .
COPY src/ src/

# 直接编译，让 Maven 自动处理依赖下载
RUN mvn package -DskipTests -B

# ==================== Stage 2: 运行阶段 ====================
FROM eclipse-temurin:17-jdk

WORKDIR /app
RUN mkdir -p /app/keystore /app/data /app/logs

# 从编译阶段复制 JAR
COPY --from=builder /build/target/nexusarchive-backend-*.jar app.jar

# 复制中文字体
COPY --from=builder /build/src/main/resources/fonts/NotoSansSC-Regular.ttf /usr/share/fonts/truetype/noto/

# 复制 keystore
COPY keystore/* /app/keystore/

ENV DB_HOST=nexus-db \
    DB_PORT=5432 \
    DB_NAME=nexusarchive \
    DB_USER=postgres \
    DB_PASS=postgres \
    REDIS_HOST=nexus-redis \
    JAVA_OPTS="-Xms256m -Xmx768m -XX:+UseG1GC"

EXPOSE 19090

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
