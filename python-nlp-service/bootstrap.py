import os
import subprocess

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# 불용어 DB 삽입
print("불용어 DB에 삽입 중...")
subprocess.run(
    ["python", "scripts/insert_stopwords_to_db.py"],
    check=True,
    cwd=BASE_DIR
)

# TF-IDF 모델 학습
print("TF-IDF 모델 학습 중...")
subprocess.run(
    ["python", "scripts/train_tfidf_model.py"],
    check=True,
    cwd=BASE_DIR
)

# FastAPI 서버 실행
print("FastAPI 서버 실행 중...")
subprocess.run(
    ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "5000"],
    cwd=BASE_DIR
)
