# 1. 베이스 이미지
FROM python:3.11-slim

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. 의존성 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 4. 전체 소스 복사
COPY . .

# 5. bootstrap.py 실행 (초기화 + uvicorn 실행)
CMD ["python", "bootstrap.py"]