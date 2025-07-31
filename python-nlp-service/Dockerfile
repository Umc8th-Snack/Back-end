# 1. 베이스 이미지
FROM python:3.11-slim

# 2. Java 설치
RUN apt update && apt install -y default-jdk && \
    apt clean && rm -rf /var/lib/apt/lists/*

# 3. JVM 경로 설정
ENV JAVA_HOME=/usr/lib/jvm/default-java
ENV PATH="$JAVA_HOME/bin:$PATH"

# 4. 작업 디렉토리 설정
WORKDIR /app

# 5. 파이썬 의존성 설치
COPY python-nlp-service/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# 6. 전체 소스 복사
COPY python-nlp-service/ .

# 7. 부트스트랩 실행
CMD ["python", "bootstrap.py"]
