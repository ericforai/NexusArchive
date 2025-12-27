# Input: 基础镜像与构建上下文
# Output: 容器镜像构建指令
# Pos: 容器构建入口
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

FROM eclipse-temurin:17-jdk

WORKDIR /app

# 预创建目录
RUN mkdir -p /app/keystore /app/data /app/logs

# 复制 JAR (假设已经 mvn package)
COPY target/nexusarchive-backend-2.0.0.jar app.jar
# 复制 中文字体 (解决 PDF 乱码问题)
RUN mkdir -p /usr/share/fonts/truetype/noto
COPY src/main/resources/fonts/NotoSansSC-Regular.ttf /usr/share/fonts/truetype/noto/NotoSansSC-Regular.ttf
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
