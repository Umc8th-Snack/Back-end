# nlp_processor.py
import pickle
import re
import pymysql
from konlpy.tag import Okt # 형태소 분석기 (예시)
from sklearn.feature_extraction.text import TfidfVectorizer
import numpy as np

import config # 설정 파일 임포트

# --- 전역 변수 (애플리케이션 시작 시 한 번만 로드) ---
okt = None # Okt 형태소 분석기 인스턴스
tfidf_vectorizer = None # 학습된 TF-IDF Vectorizer 모델
easy_words_set = set() # '쉬운 단어' 집합 (메모리 로드)
stopwords_set = set() # '불용어' 집합 (메모리 로드)

# --- 모델 및 데이터 로드 함수 (FastAPI startup 이벤트에서 호출) ---
def load_models_and_data():
    global okt, tfidf_vectorizer, easy_words_set, stopwords_set

    try:
        # 1. 형태소 분석기 초기화
        okt = Okt() 
        print("Okt 형태소 분석기 초기화 완료.")

        # 2. TF-IDF Vectorizer 모델 로드
        # config.TFIDF_MODEL_PATH는 config.py에서 정의된 경로를 사용합니다.
        with open(config.TFIDF_MODEL_PATH, 'rb') as f:
            tfidf_vectorizer = pickle.load(f)
        print(f"TF-IDF Vectorizer 로드 완료: {config.TFIDF_MODEL_PATH}")

        # 3. '쉬운 단어'와 '불용어'를 데이터베이스에서 로드
        conn = pymysql.connect(
            host=config.DB_HOST,
            port=config.DB_PORT,
            user=config.DB_USER,
            password=config.DB_PASSWORD,
            db=config.DB_NAME,
            charset='utf8mb4'
        )
        cursor = conn.cursor()

        # 'easy_words' 테이블에서 'word' 컬럼의 모든 단어 로드
        # 해당 테이블과 컬럼이 DB에 존재해야 합니다.
        cursor.execute("SELECT word FROM easy_words") 
        easy_words_set = {row[0] for row in cursor.fetchall()}
        print(f"DB에서 쉬운 단어 로드 완료. 개수: {len(easy_words_set)}")

        # 'stopwords' 테이블에서 'word' 컬럼의 모든 단어 로드
        # 해당 테이블과 컬럼이 DB에 존재해야 합니다.
        cursor.execute("SELECT word FROM stopwords") 
        stopwords_set = {row[0] for row in cursor.fetchall()}
        print(f"DB에서 불용어 로드 완료. 개수: {len(stopwords_set)}")

        cursor.close()
        conn.close()
        print("DB 리소스 로드 완료.")

    except FileNotFoundError as e:
        print(f"오류: 필수 모델/데이터 파일이 없습니다: {e.filename}. 'data/' 디렉토리에 파일이 있는지 확인하세요.")
        raise RuntimeError(f"필수 파일 누락: {e.filename}") from e
    except pymysql.Error as e:
        print(f"DB 연결 또는 쿼리 오류 발생: {e}. DB 인증 정보와 테이블 이름을 확인하세요.")
        raise RuntimeError(f"DB에서 NLP 리소스 로드 실패: {e}") from e
    except Exception as e:
        print(f"NLP 리소스 로드 중 예상치 못한 오류 발생: {e}")
        raise RuntimeError(f"NLP 리소스 로드 실패: {e}") from e

# --- 텍스트 전처리 공통 함수 ---
def preprocess_text_for_nlp(text: str) -> str:
    """텍스트에서 특수문자를 제거하고 공백을 정규화합니다."""
    # 한글, 영어, 숫자, 공백을 제외한 모든 문자 제거
    text = re.sub(r'[^가-힣a-zA-Z0-9\s]', '', text) 
    # 여러 공백을 단일 공백으로 치환하고, 앞뒤 공백 제거
    text = re.sub(r'\s+', ' ', text).strip() 
    return text

# --- 형태소 분석 함수 ---
def analyze_morphs(text: str) -> list[tuple[str, str]]:
    """입력 텍스트를 형태소 분석하고 품사를 태깅합니다."""
    if okt is None:
        raise RuntimeError("형태소 분석기 (Okt)가 초기화되지 않았습니다. load_models_and_data()를 먼저 호출하세요.")
    
    processed_text = preprocess_text_for_nlp(text)
    if not processed_text:
        return []
        
    # norm=True: 정규화 (예: 'ㅋㅋㅋ' -> 'ㅋㅋ'), stem=True: 어간 추출 (예: '먹었다' -> '먹다')
    return okt.pos(processed_text, norm=True, stem=True)

# --- 어려운 단어 추출 로직 ---
def extract_difficult_words(content: str) -> list[str]:
    """기사 내용에서 '어려운 단어'를 추출합니다."""
    tokens_with_pos = analyze_morphs(content)

    difficult_words = []
    # 후보 단어 필터링: 명사(N*), 동사 어간(V*), 형용사 어간(A*)만 고려
    # 불용어 제거 및 한 글자 단어 제외
    for word, pos in tokens_with_pos:
        if (pos.startswith('N') or pos.startswith('V') or pos.startswith('A')) and \
           word not in stopwords_set and len(word) > 1:
            
            if word not in easy_words_set: # '쉬운 단어' 집합에 없으면 어려운 단어
                difficult_words.append(word)
                
    # (선택 사항) 명사 조합 판단: "실질" + "소득" -> "실질소득"과 같이 복합 명사 처리 로직을 여기에 추가 가능
    # 이는 형태소 분석기의 결과와 품사 태그(NNG, NNP 등)를 활용하여 구현할 수 있습니다.
    
    return list(set(difficult_words)) # 중복 제거 후 반환

# --- TF-IDF 벡터화 로직 ---
def vectorize_tfidf(text: str) -> list[float]:
    """입력 텍스트를 TF-IDF 벡터로 변환합니다."""
    if tfidf_vectorizer is None:
        raise RuntimeError("TF-IDF Vectorizer가 로드되지 않았습니다. load_models_and_data()를 먼저 호출하세요.")

    # 텍스트를 형태소 분석 후 명사만 추출하여 공백으로 조인
    # TfidfVectorizer의 입력 형식 (공백으로 구분된 토큰 문자열)에 맞게 변환
    tokens_with_pos = analyze_morphs(text)
    processed_tokens = [word for word, pos in tokens_with_pos if pos.startswith('N')] # 명사만 추출
    text_for_vectorizer = ' '.join(processed_tokens)

    if not text_for_vectorizer: # 처리할 유효한 토큰이 없으면 빈 벡터 반환
        return []

    # 학습된 TfidfVectorizer를 사용하여 벡터화 수행
    # .transform() 메서드는 희소 행렬(sparse matrix)을 반환합니다.
    vector = tfidf_vectorizer.transform([text_for_vectorizer])
    
    # 희소 행렬을 밀집 행렬(dense array)로 변환하고 파이썬 리스트로 반환
    # 이 형식은 Java에서 JSON으로 사용하기 용이합니다.
    return vector.toarray()[0].tolist()
