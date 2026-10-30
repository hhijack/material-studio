# 注意：本 Dockerfile 以【仓库根】为构建上下文（compose 中 backend.build.context: .），
# 因为 pom 的 copy-db-init 需要仓库根的 db/init（构建上下文无法引用上级目录，缺失时
# maven-resources 会静默跳过，导致 jar 缺少建表 SQL、后端启动失败——见 04-I-1 / V-011）。
# ---- 构建阶段 ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先只拷 pom 以下载依赖（利用层缓存）
COPY backend/pom.xml .
RUN mvn -q -B dependency:go-offline

COPY backend/src ./src
# 建表/种子 SQL 单一来源（与 compose 挂载给 MySQL 初始化的是同一份）
COPY db/init /build/db-init
RUN mvn -q -B package -DskipTests -Ddbinit.dir=/build/db-init

# ---- 运行阶段 ----
FROM eclipse-temurin:17-jre-jammy
# fonts-noto-cjk：结果图中文文案渲染（极端缺字体时 ResultComposer 自动降级英文标题）
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl fonts-noto-cjk \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /build/target/material-studio-backend-1.0.0.jar app.jar

ENV DATA_DIR=/app/data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
