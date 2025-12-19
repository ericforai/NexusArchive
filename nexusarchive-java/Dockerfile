FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 预创建目录
RUN mkdir -p /app/keystore /app/data /app/logs

# 复制 JAR (假设已经 mvn package)
COPY target/*.jar app.jar
COPY keystore/*.pem /app/keystore/

# 环境变量默认值 (对应 docker-compose)
ENV DB_HOST=nexus-db \
    DB_PORT=5432 \
    DB_NAME=nexusarchive \
    DB_USER=postgres \
    DB_PASS=postgres \
    REDIS_HOST=nexus-redis

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
