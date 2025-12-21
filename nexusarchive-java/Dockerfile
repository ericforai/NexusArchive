FROM eclipse-temurin:17-jdk

WORKDIR /app

# 预创建目录
RUN mkdir -p /app/keystore /app/data /app/logs

# 复制 JAR (假设已经 mvn package)
COPY target/nexusarchive-backend-2.0.0.jar app.jar
# 复制 keystore (如果存在，否则依赖 volume 挂载)
COPY keystore/* /app/keystore/

# 环境变量默认值 (对应 docker-compose)
ENV DB_HOST=nexus-db \
    DB_PORT=5432 \
    DB_NAME=nexusarchive \
    DB_USER=postgres \
    DB_PASS=postgres \
    REDIS_HOST=nexus-redis

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
