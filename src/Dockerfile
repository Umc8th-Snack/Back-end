# Java 21 JDK (경량 Alpine 버전)
FROM eclipse-temurin:21-jdk-alpine

# 작업 디렉토리 생성
WORKDIR /app

# 빌드된 JAR 복사
COPY /home/ubuntu/snack-0.0.1-SNAPSHOT.jar app.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
