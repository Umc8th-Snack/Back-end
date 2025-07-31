# 1. 베이스 이미지
FROM python:3.11-slim

# 2. 시스템 패키지 업데이트 및 Mecab 설치 (한국어용)
RUN apt-get update && \
    apt-get install -y curl git make build-essential \
    mecab libmecab-dev mecab-ipadic-utf8 \
    mecab-ko mecab-ko-dic \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# 3. 작업 디렉토리 설정
WORKDIR /app

# 4. 의존성 설치
COPY python-nlp-service/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# 5. 전체 소스 복사
COPY python-nlp-service/ .

# 6. 실행
CMD ["python", "bootstrap.py"]
