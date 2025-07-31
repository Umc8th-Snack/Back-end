# 1. 베이스 이미지
FROM python:3.11-slim

# 2. 시스템 패키지 업데이트 및 Java 설치
RUN apt update && apt install -y openjdk-11-jdk && \
    apt clean && rm -rf /var/lib/apt/lists/*

# 3. JAVA_HOME 환경 변수 설정
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# 4. 작업 디렉토리 설정
WORKDIR /app

# 5. 파이썬 의존성 설치
COPY python-nlp-service/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# 6. 전체 소스 복사
COPY python-nlp-service/ .

# 7. 부트스트랩 실행
CMD ["python", "bootstrap.py"]
