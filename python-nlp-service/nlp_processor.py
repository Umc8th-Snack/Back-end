import pickle
import re
import os
from sklearn.feature_extraction.text import TfidfVectorizer
from scripts.tokenizer_utils import dummy_tokenizer
from dotenv import load_dotenv
import pymysql
import csv

# .env 로드
load_dotenv(os.path.join(os.path.dirname(__file__), '..', '.env'))

TFIDF_MODEL_PATH = os.path.join(os.path.dirname(__file__), '..', 'data', 'tfidf_vectorizer.pkl')
STOPWORDS_PATH = os.path.join(os.path.dirname(__file__), 'data_source', 'stopwords.csv')

DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_PORT = int(os.getenv('DB_PORT', 3306))
DB_USER = os.getenv('DB_USER', 'root')
DB_PASSWORD = os.getenv('DB_PASSWORD', '')
DB_NAME = os.getenv('DB_NAME', '')

# --- 불용어 로딩 (CSV 파일 기반으로 변경) ---
stopwords_set = set()
try:
    with open(STOPWORDS_PATH, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        stopwords_set = {row[0].strip() for row in reader if row and row[0].strip()}
    print(f"불용어 CSV에서 {len(stopwords_set)}개 불러옴")
except Exception as e:
    print(f"경고: 불용어 CSV 로드 실패: {e}")
    stopwords_set = set()

# --- 간단한 토크나이저 (띄어쓰기 기반, 불용어 제거 포함) ---
def tokenize_korean_text_for_tfidf(text: str) -> list[str]:
    text = re.sub(r'[^가-힣a-zA-Z0-9\s]', '', text)
    text = re.sub(r'\s+', ' ', text).strip()
    tokens = text.split()
    return [
        word for word in tokens
        if word not in stopwords_set and len(word) > 1
    ]

# --- TF-IDF 학습 ---
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
