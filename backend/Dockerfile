# Backend Spring Boot (monolith: REST + WebSocket + server TCP + job) — bước 13.
# Build: docker compose build backend  (hoặc docker build -t ielts-backend ./backend)

# ---- Giai đoạn build: tải thư viện trước để Docker cache lại khi chỉ đổi code ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package && cp target/ielts-*.jar /app.jar

# ---- Giai đoạn chạy: chỉ JRE, chạy bằng user thường ----
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 --home /app app \
    && mkdir -p /app /data/uploads && chown -R app /app /data/uploads
WORKDIR /app
COPY --from=build /app.jar app.jar
USER app
ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=UTC"
# 8080: HTTP + WebSocket (chỉ nginx gọi tới) · 9090: server TCP import (admin-cli)
EXPOSE 8080 9090
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
