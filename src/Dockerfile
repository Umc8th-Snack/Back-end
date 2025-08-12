# --- Build stage ---
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Gradle 캐시 최적화
COPY gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies > /dev/null 2>&1 || true

# 소스 복사 후 빌드
COPY . .
RUN ./gradlew bootJar -x test

# --- Run stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 필요 시 타임존:
# RUN apk add --no-cache tzdata && ln -snf /usr/share/zoneinfo/Asia/Seoul /etc/localtime

# 산출물 복사 (단일 JAR 가정)
COPY --from=builder /app/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
