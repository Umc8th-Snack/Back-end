# scripts/train_tfidf_model.py
import pickle
import re
import pymysql
from konlpy.tag import Okt
from sklearn.feature_extraction.text import TfidfVectorizer
import os
from dotenv import load_dotenv

# .env 파일에서 환경 변수를 로드합니다. (스크립트 실행 경로 기준)
# scripts 디렉토리에서 실행될 때, .env 파일은 상위 디렉토리(python-nlp-service/)에 있습니다.
load_dotenv(os.path.join(os.path.dirname(__file__), '..', '.env'))

# --- 설정 파일 경로 (config.py와 동일하게 설정) ---
# 이 스크립트가 config.py를 직접 임포트해도 되지만, 여기서는 상대 경로를 명시
TFIDF_MODEL_PATH = os.path.join(os.path.dirname(__file__), '..', 'data', 'tfidf_vectorizer.pkl')

# --- DB 설정 (실제 프로젝트 config.py와 동일하게 설정) ---
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_PORT = int(os.getenv('DB_PORT', 3306))
DB_USER = os.getenv('DB_USER', 'root')
DB_PASSWORD = os.getenv('DB_PASSWORD', 'your_password') # ⭐ .env 파일에서 로드되도록 설정
DB_NAME = os.getenv('DB_NAME', 'your_database') # ⭐ .env 파일에서 로드되도록 설정

# --- 형태소 분석기 초기화 (학습 스크립트용) ---
okt = Okt()

def tokenize_korean_text_for_tfidf(text: str) -> list[str]:
    """TF-IDF 벡터화를 위한 텍스트 전처리 및 명사 추출"""
    text = re.sub(r'[^가-힣a-zA-Z0-9\s]', '', text)
    text = re.sub(r'\s+', ' ', text).strip()
    
    tokens = okt.pos(text, norm=True, stem=True)
    return [word for word, pos in tokens if pos.startswith('N')] # 명사만 추출


def dummy_tokenizer(text: str) -> list[str]:
    """TfidfVectorizer의 tokenizer 인자로 사용하기 위한 래퍼 함수"""
    return tokenize_korean_text_for_tfidf(text)

# --- TF-IDF 모델 학습 및 저장 ---
def train_and_save_tfidf_model(article_contents: list[str]):
    """
    주어진 기사 본문 리스트로 TF-IDF 모델을 학습하고 직렬화하여 파일에 저장합니다.
    """
    if not article_contents:
        print("TF-IDF 학습을 위한 기사 내용이 제공되지 않았습니다. 모델 학습을 건너뜁니다.")
        return

    print("TF-IDF 모델 학습 시작...")
    tfidf_vectorizer = TfidfVectorizer(tokenizer=dummy_tokenizer,
                                       min_df=5, # 최소 5개 문서에 나타나야 단어로 간주
                                       max_df=0.8) # 80% 이상 문서에 나타나면 흔한 단어로 간주 (불용어 효과)

    tfidf_vectorizer.fit(article_contents)
    print(f"TF-IDF 모델 학습 완료. 어휘 사전 크기: {len(tfidf_vectorizer.vocabulary_)}")

    # 모델 저장 디렉토리가 없으면 생성
    os.makedirs(os.path.dirname(TFIDF_MODEL_PATH), exist_ok=True)
    with open(TFIDF_MODEL_PATH, 'wb') as f:
        pickle.dump(tfidf_vectorizer, f)
    print(f"TF-IDF 모델이 다음 경로에 저장되었습니다: {TFIDF_MODEL_PATH}")

# --- 메인 실행 로직 ---
if __name__ == "__main__":
    print("--- TF-IDF 모델 학습 스크립트 실행 ---")

    # ⭐ 실제 Article DB에서 'content' 필드를 모두 가져와서 리스트로 만들기 ⭐
    # 이 부분에 DB 연결 및 쿼리 로직을 구현해야 합니다.
    article_contents_from_db = []
    conn = None
    try:
        conn = pymysql.connect(host=DB_HOST, port=DB_PORT, user=DB_USER, password=DB_PASSWORD, db=DB_NAME, charset='utf8mb4')
        cursor = conn.cursor()
        cursor.execute("SELECT content FROM articles") # 'articles' 테이블에 'content' 컬럼이 있다고 가정
        article_contents_from_db = [row[0] for row in cursor.fetchall()]
        cursor.close()
        conn.close()
        print(f"DB에서 {len(article_contents_from_db)}개의 기사 내용을 로드했습니다.")
    except pymysql.Error as e:
        print(f"오류: TF-IDF 학습을 위해 DB에서 기사 내용을 로드할 수 없습니다: {e}")
        print("DB 설정(.env 파일)이 올바른지, 'articles' 테이블이 존재하는지 확인하세요.")
        exit(1) # DB 로드 실패 시 스크립트 종료
    except Exception as e:
        print(f"예상치 못한 오류 발생: {e}")
        exit(1)

    # TF-IDF 모델 학습 및 저장
    train_and_save_tfidf_model(article_contents_from_db)

    print("--- TF-IDF 모델 학습 스크립트 완료 ---")
