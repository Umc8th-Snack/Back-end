# scripts/train_tfidf_model.py
import pickle
import re
import pymysql
from sklearn.feature_extraction.text import TfidfVectorizer
import os
from dotenv import load_dotenv
from .tokenizer_utils import dummy_tokenizer

# Mecab 사용
from konlpy.tag import Mecab
mecab = Mecab()

# .env 파일에서 환경 변수 로드
load_dotenv(os.path.join(os.path.dirname(__file__), '..', '.env'))

# --- 설정 ---
TFIDF_MODEL_PATH = os.path.join(os.path.dirname(__file__), '..', 'data', 'tfidf_vectorizer.pkl')

DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_PORT = int(os.getenv('DB_PORT', 3306))
DB_USER = os.getenv('DB_USER', 'root')
DB_PASSWORD = os.getenv('DB_PASSWORD', 'your_password')
DB_NAME = os.getenv('DB_NAME', 'your_database')

# --- 불용어 로딩 ---
stopwords_set = set()
try:
    conn = pymysql.connect(
        host=DB_HOST, port=DB_PORT, user=DB_USER, password=DB_PASSWORD, db=DB_NAME, charset='utf8mb4'
    )
    cursor = conn.cursor()
    cursor.execute("SELECT word FROM stopwords")
    stopwords_set = {row[0] for row in cursor.fetchall()}
    cursor.close()
    conn.close()
    print(f"학습 스크립트에서 불용어 로드 완료. 개수: {len(stopwords_set)}")
except pymysql.Error as e:
    print(f"경고: 불용어 DB 로드 실패: {e}. 불용어 제거 없이 진행.")
    stopwords_set = set()

# --- 텍스트 전처리 및 토크나이징 ---
def tokenize_korean_text_for_tfidf(text: str) -> list[str]:
    text = re.sub(r'[^가-힣a-zA-Z0-9\s]', '', text)
    text = re.sub(r'\s+', ' ', text).strip()
    tokens = mecab.pos(text)
    return [
        word for word, pos in tokens
        if pos.startswith('N') and word not in stopwords_set and len(word) > 1
    ]

# --- 모델 학습 함수 ---
def train_and_save_tfidf_model(article_contents: list[str]):
    if not article_contents:
        print("TF-IDF 학습을 위한 데이터 없음. 학습 스킵.")
        return

    print("TF-IDF 모델 학습 시작...")
    tfidf_vectorizer = TfidfVectorizer(
        tokenizer=tokenize_korean_text_for_tfidf,
        min_df=2,
        max_df=0.8
    )
    tfidf_vectorizer.fit(article_contents)
    print(f"학습 완료. 어휘 수: {len(tfidf_vectorizer.vocabulary_)}")

    os.makedirs(os.path.dirname(TFIDF_MODEL_PATH), exist_ok=True)
    with open(TFIDF_MODEL_PATH, 'wb') as f:
        pickle.dump(tfidf_vectorizer, f)
    print(f"모델 저장 완료: {TFIDF_MODEL_PATH}")

# --- 메인 실행 ---
if __name__ == "__main__":
    print("--- TF-IDF 모델 학습 스크립트 실행 ---")

    article_contents_from_db = []
    try:
        conn = pymysql.connect(
            host=DB_HOST, port=DB_PORT, user=DB_USER, password=DB_PASSWORD, db=DB_NAME, charset='utf8mb4'
        )
        cursor = conn.cursor()
        cursor.execute("SELECT summary FROM articles")
        rows = cursor.fetchall()
        article_contents_from_db = [
            row[0].strip() for row in rows if row[0] and isinstance(row[0], str) and row[0].strip()
        ]
        cursor.close()
        conn.close()
        print(f"DB에서 {len(article_contents_from_db)}개의 summary 로드 완료.")
    except pymysql.Error as e:
        print(f"DB 에러: {e}")
        exit(1)
    except Exception as e:
        print(f"예상치 못한 오류: {e}")
        exit(1)

    train_and_save_tfidf_model(article_contents_from_db)

    print("--- TF-IDF 모델 학습 스크립트 완료 ---")
