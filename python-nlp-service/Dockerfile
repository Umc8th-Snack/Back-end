# 1. 베이스 이미지
FROM python:3.11-slim

# 2. Java 설치
RUN apt update && apt install -y default-jdk && \
    apt clean && rm -rf /var/lib/apt/lists/*

# 3. JAVA 환경변수 설정
ENV JAVA_HOME=/usr/lib/jvm/default-java
ENV PATH=$JAVA_HOME/bin:$PATH

# 4. 작업 디렉토리 설정
WORKDIR /app

# PYTHON-NLP-SERVICE 디렉토리를 PYTHONPATH에 추가
ENV PYTHONPATH=/app:$PYTHONPATH

# 5. requirements 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 6. 전체 소스 복사
COPY . .

# 7. FastAPI & 전처리 실행
CMD ["python", "bootstrap.py"]