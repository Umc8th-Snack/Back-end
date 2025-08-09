import os
import subprocess
import sys



BASE_DIR = os.path.dirname(os.path.abspath(__file__))


print("--- DEBUG INFO START ---")
print(f"Current Working Directory: {os.getcwd()}")
print(f"sys.path:")
for path in sys.path:
    print(f"  - {path}")
print("--- DEBUG INFO END ---")

# 불용어 DB 삽입
print("불용어 DB에 삽입 중...")
subprocess.run(
    ["python", "-m", "scripts.insert_stopwords_to_db"],
    check=True,
    cwd=BASE_DIR
)

# TF-IDF 모델 학습
print("TF-IDF 모델 학습 중...")
subprocess.run(
    ["python", "-m", "scripts.train_tfidf_model"],
    check=True,
    cwd=BASE_DIR
)

# FastAPI 서버 실행
print("FastAPI 서버 실행 중...")
subprocess.run(
    ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "5000"],
    cwd=BASE_DIR
)
