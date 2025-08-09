# nlp_processor.py
import pickle
import re
import os
import asyncio
import logging
from typing import List, Tuple, Optional
import numpy as np
import pymysql
from dotenv import load_dotenv
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from sentence_transformers import SentenceTransformer
import json
from concurrent.futures import ThreadPoolExecutor

# 로깅 설정
logger = logging.getLogger(__name__)

# 환경 변수 로드
load_dotenv(os.path.join(os.path.dirname(__file__), '.env'))

# 설정 상수
TFIDF_MODEL_PATH = os.path.join(os.path.dirname(__file__), 'data', 'tfidf_vectorizer.pkl')
SBERT_MODEL_NAME = "jhgan/ko-sroberta-multitask"  # 한국어 SBERT 모델
STOPWORDS_PATH = os.path.join(os.path.dirname(__file__), 'scripts', 'data_source', 'stopwords.csv')

# DB 설정
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'port': int(os.getenv('DB_PORT', 3306)),
    'user': os.getenv('DB_USER', 'root'),
    'password': os.getenv('DB_PASSWORD', ''),
    'db': os.getenv('DB_NAME', ''),
    'charset': 'utf8mb4'
}

# 전역 변수들
tfidf_vectorizer: Optional[TfidfVectorizer] = None
sbert_model: Optional[SentenceTransformer] = None
stopwords_set: set = set()
service_ready: bool = False
executor = ThreadPoolExecutor(max_workers=4)

# --- 불용어 로딩 ---
def load_stopwords():
    global stopwords_set
    try:
        import csv
        with open(STOPWORDS_PATH, 'r', encoding='utf-8') as f:
            reader = csv.reader(f)
            stopwords_set = {row[0].strip() for row in reader if row and row[0].strip()}
        logger.info(f"불용어 {len(stopwords_set)}개 로드 완료")
    except Exception as e:
        logger.warning(f"불용어 로드 실패: {e}")
        stopwords_set = set()

# --- 텍스트 전처리 및 토크나이징 ---
def tokenize_korean_text(text: str) -> List[str]:
    """한국어 텍스트를 토크나이징합니다."""
    if not text:
        return []

    # 특수문자 제거 및 정규화
    text = re.sub(r'[^가-힣a-zA-Z0-9\s]', '', text)
    text = re.sub(r'\s+', ' ', text).strip()

    # 단순 공백 기반 토크나이징 (실제로는 KoNLPy 등을 사용하는 것이 좋습니다)
    tokens = text.split()

    # 불용어 제거 및 길이 필터링
    return [
        word for word in tokens
        if word not in stopwords_set and len(word) > 1
    ]

# --- DB 연결 함수 ---
def get_db_connection():
    """DB 연결을 반환합니다."""
    return pymysql.connect(**DB_CONFIG)

# --- TF-IDF 모델 로드/학습 ---
async def load_or_train_tfidf_model():
    """TF-IDF 모델을 로드하거나 학습합니다."""
    global tfidf_vectorizer

    if os.path.exists(TFIDF_MODEL_PATH):
        logger.info("기존 TF-IDF 모델 로딩...")
        with open(TFIDF_MODEL_PATH, 'rb') as f:
            tfidf_vectorizer = pickle.load(f)
        logger.info(f"TF-IDF 모델 로드 완료. 어휘 수: {len(tfidf_vectorizer.vocabulary_)}")
    else:
        logger.info("TF-IDF 모델이 없습니다. 새로 학습합니다...")
        await train_tfidf_model()

async def train_tfidf_model():
    """DB에서 데이터를 가져와 TF-IDF 모델을 학습합니다."""
    global tfidf_vectorizer

    def _train_model():
        # DB에서 summary 데이터 가져오기
        conn = get_db_connection()
        try:
            with conn.cursor() as cursor:
                cursor.execute("SELECT summary FROM articles WHERE summary IS NOT NULL AND summary != ''")
                rows = cursor.fetchall()
                summaries = [row[0].strip() for row in rows if row[0] and row[0].strip()]
        finally:
            conn.close()

        if not summaries:
            raise RuntimeError("학습할 데이터가 없습니다.")

        logger.info(f"TF-IDF 학습 데이터: {len(summaries)}개")

        # TF-IDF 벡터라이저 학습
        tfidf_vectorizer = TfidfVectorizer(
            tokenizer=tokenize_korean_text,
            min_df=2,
            max_df=0.8,
            max_features=10000  # 최대 특성 수 제한
        )

        tfidf_vectorizer.fit(summaries)

        # 모델 저장
        os.makedirs(os.path.dirname(TFIDF_MODEL_PATH), exist_ok=True)
        with open(TFIDF_MODEL_PATH, 'wb') as f:
            pickle.dump(tfidf_vectorizer, f)

        logger.info(f"TF-IDF 모델 학습 완료. 어휘 수: {len(tfidf_vectorizer.vocabulary_)}")

    # 별도 스레드에서 실행
    loop = asyncio.get_event_loop()
    await loop.run_in_executor(executor, _train_model)

# --- SBERT 모델 로드 ---
async def load_sbert_model():
    """SBERT 모델을 로드합니다."""
    global sbert_model

    def _load_model():
        return SentenceTransformer(SBERT_MODEL_NAME)

    logger.info("SBERT 모델 로딩...")
    loop = asyncio.get_event_loop()
    sbert_model = await loop.run_in_executor(executor, _load_model)
    logger.info("SBERT 모델 로드 완료")

# --- 서비스 초기화 ---
async def initialize_nlp_service():
    """NLP 서비스를 초기화합니다."""
    global service_ready

    try:
        logger.info("불용어 로딩 중...")
        load_stopwords()

        logger.info("TF-IDF 모델 로딩 중...")
        await load_or_train_tfidf_model()

        logger.info("SBERT 모델 로딩 중...")
        await load_sbert_model()

        service_ready = True
        logger.info("NLP 서비스 초기화 완료")

    except Exception as e:
        logger.error(f"NLP 서비스 초기화 실패: {e}")
        service_ready = False
        # 개발 중에는 예외를 발생시키지 않고 로깅만 함
        logger.warning("서비스는 계속 실행되지만 NLP 기능이 제한됩니다.")

# --- 텍스트 벡터화 ---
async def vectorize_text(text: str) -> Tuple[dict, dict]:
    """텍스트를 TF-IDF 키워드 딕셔너리와 키워드별 SBERT 벡터 딕셔너리로 변환합니다."""
    if not service_ready:
        raise RuntimeError("NLP 서비스가 초기화되지 않았습니다.")

    def _vectorize():
        # TF-IDF 벡터화
        tfidf_matrix = tfidf_vectorizer.transform([text])
        tfidf_vector = tfidf_matrix.toarray()[0]

        # TF-IDF 값이 0 이상인 단어들과 점수 추출
        feature_names = tfidf_vectorizer.get_feature_names_out()
        tfidf_keywords = {}

        for idx, score in enumerate(tfidf_vector):
            if score > 0:
                keyword = feature_names[idx]
                tfidf_keywords[keyword] = float(score)

        # 키워드들을 점수 순으로 정렬
        tfidf_keywords = dict(sorted(tfidf_keywords.items(), key=lambda x: x[1], reverse=True))

        # 각 키워드별로 SBERT 벡터 생성
        sbert_keywords = {}
        if tfidf_keywords:
            # 키워드들을 리스트로 변환하여 배치 처리
            keywords_list = list(tfidf_keywords.keys())
            sbert_vectors = sbert_model.encode(keywords_list)

            # 각 키워드와 벡터를 매핑
            for keyword, vector in zip(keywords_list, sbert_vectors):
                sbert_keywords[keyword] = vector.tolist()

        return tfidf_keywords, sbert_keywords

    loop = asyncio.get_event_loop()
    return await loop.run_in_executor(executor, _vectorize)

# --- 모든 기사 벡터화 처리 ---
async def process_all_articles(reprocess: bool = False) -> Tuple[int, int]:
    """DB의 모든 기사를 벡터화하여 분리된 테이블에 저장합니다."""
    if not service_ready:
        raise RuntimeError("NLP 서비스가 초기화되지 않았습니다.")

    def _process_articles():
        conn = get_db_connection()
        try:
            with conn.cursor() as cursor:
                # 처리할 기사들 조회
                if reprocess:
                    query = """
                    SELECT a.article_id, a.summary FROM articles a
                    WHERE a.summary IS NOT NULL AND a.summary != ''
                    """
                else:
                    query = """
                    SELECT a.article_id, a.summary FROM articles a
                    LEFT JOIN article_semantic_vectors asv ON a.article_id = asv.article_id
                    LEFT JOIN article_tfidf_vectors atv ON a.article_id = atv.article_id
                    WHERE a.summary IS NOT NULL AND a.summary != ''
                    AND (asv.article_id IS NULL OR atv.article_id IS NULL)
                    """

                cursor.execute(query)
                articles = cursor.fetchall()

                total_count = len(articles)
                processed_count = 0

                logger.info(f"처리할 기사 수: {total_count}")

                # 배치로 처리
                batch_size = 30  # SBERT 처리로 인해 배치 크기 감소
                feature_names = tfidf_vectorizer.get_feature_names_out()

                for i in range(0, len(articles), batch_size):
                    batch = articles[i:i + batch_size]

                    for article_id, summary in batch:
                        try:
                            # TF-IDF 벡터화
                            tfidf_matrix = tfidf_vectorizer.transform([summary])
                            tfidf_vector = tfidf_matrix.toarray()[0]

                            # TF-IDF 값이 0 이상인 단어들과 점수 추출
                            tfidf_keywords = {}
                            for idx, score in enumerate(tfidf_vector):
                                if score > 0:
                                    keyword = feature_names[idx]
                                    tfidf_keywords[keyword] = float(score)

                            # 키워드들을 점수 순으로 정렬
                            tfidf_keywords = dict(sorted(tfidf_keywords.items(), key=lambda x: x[1], reverse=True))

                            # 각 키워드별로 SBERT 벡터 생성
                            sbert_keywords = {}
                            if tfidf_keywords:
                                keywords_list = list(tfidf_keywords.keys())
                                sbert_vectors = sbert_model.encode(keywords_list)

                                for keyword, vector in zip(keywords_list, sbert_vectors):
                                    sbert_keywords[keyword] = vector.tolist()

                            # JSON으로 변환
                            tfidf_keywords_json = json.dumps(tfidf_keywords, ensure_ascii=False)
                            sbert_keywords_json = json.dumps(sbert_keywords, ensure_ascii=False)
                            full_tfidf_json = json.dumps(tfidf_vector.tolist())

                            # 디버깅을 위한 로깅
                            logger.debug(f"기사 {article_id}: TF-IDF 키워드 {len(tfidf_keywords)}개, SBERT 벡터 {len(sbert_keywords)}개")

                            # TF-IDF 벡터 저장/업데이트 (전체 벡터 저장)
                            tfidf_upsert_query = """
                            INSERT INTO article_tfidf_vectors (article_id, vector, updated_at)
                            VALUES (%s, %s, NOW())
                            ON DUPLICATE KEY UPDATE 
                            vector = VALUES(vector), updated_at = VALUES(updated_at)
                            """
                            cursor.execute(tfidf_upsert_query, (article_id, full_tfidf_json))

                            # SBERT 키워드별 벡터 저장/업데이트
                            # vector 컬럼: 키워드별 SBERT 벡터 딕셔너리
                            # keywords 컬럼: TF-IDF 키워드 점수 딕셔너리
                            sbert_upsert_query = """
                            INSERT INTO article_semantic_vectors (article_id, vector, keywords, updated_at)
                            VALUES (%s, %s, %s, NOW())
                            ON DUPLICATE KEY UPDATE 
                            vector = VALUES(vector), keywords = VALUES(keywords), updated_at = VALUES(updated_at)
                            """
                            cursor.execute(sbert_upsert_query, (article_id, sbert_keywords_json, tfidf_keywords_json))

                            processed_count += 1

                        except Exception as e:
                            logger.error(f"기사 {article_id} 처리 실패: {e}")
                            continue

                    # 배치 커밋
                    conn.commit()
                    logger.info(f"진행률: {min(i + batch_size, total_count)}/{total_count}")

                return processed_count, total_count

        finally:
            conn.close()

    loop = asyncio.get_event_loop()
    return await loop.run_in_executor(executor, _process_articles)

# --- 유사 기사 검색 ---
async def find_similar_articles(query_text: str, top_k: int = 5,
                                similarity_threshold: float = 0.3) -> List[dict]:
    """입력 텍스트와 유사한 기사들을 찾습니다."""
    if not service_ready:
        raise RuntimeError("NLP 서비스가 초기화되지 않았습니다.")

    def _vectorize_sync(text: str):
        """동기 버전의 벡터화 함수"""
        tfidf_matrix = tfidf_vectorizer.transform([text])
        tfidf_vector = tfidf_matrix.toarray()[0]

        feature_names = tfidf_vectorizer.get_feature_names_out()
        tfidf_keywords = {}

        for idx, score in enumerate(tfidf_vector):
            if score > 0:
                keyword = feature_names[idx]
                tfidf_keywords[keyword] = float(score)

        tfidf_keywords = dict(sorted(tfidf_keywords.items(), key=lambda x: x[1], reverse=True))

        sbert_keywords = {}
        if tfidf_keywords:
            keywords_list = list(tfidf_keywords.keys())
            sbert_vectors = sbert_model.encode(keywords_list)

            for keyword, vector in zip(keywords_list, sbert_vectors):
                sbert_keywords[keyword] = vector.tolist()

        return tfidf_keywords, sbert_keywords

    def _find_similar():
        # 쿼리를 키워드별 벡터로 변환
        tfidf_keywords, sbert_keywords = _vectorize_sync(query_text)

        if not sbert_keywords:
            return []

        # DB에서 벡터가 있는 기사들 조회 (분리된 테이블에서)
        conn = get_db_connection()
        try:
            with conn.cursor() as cursor:
                cursor.execute("""
                    SELECT a.id, a.title, a.summary, asv.vector, asv.keywords
                    FROM articles a
                    INNER JOIN article_semantic_vectors asv ON a.id = asv.article_id
                    WHERE asv.vector IS NOT NULL
                """)
                articles = cursor.fetchall()
        finally:
            conn.close()

        if not articles:
            return []

        # 유사도 계산
        similarities = []
        for article_id, title, summary, sbert_vector_json, keywords_json in articles:
            try:
                # 기사의 키워드별 벡터 파싱
                article_sbert_keywords = json.loads(sbert_vector_json) if sbert_vector_json else {}

                if not article_sbert_keywords:
                    continue

                # 공통 키워드 찾기 및 유사도 계산
                common_keywords = set(sbert_keywords.keys()) & set(article_sbert_keywords.keys())

                if not common_keywords:
                    # 공통 키워드가 없으면 모든 키워드 조합으로 유사도 계산
                    max_similarity = 0.0
                    for q_keyword, q_vector in sbert_keywords.items():
                        for a_keyword, a_vector in article_sbert_keywords.items():
                            q_vec = np.array(q_vector)
                            a_vec = np.array(a_vector)
                            sim = cosine_similarity([q_vec], [a_vec])[0][0]
                            max_similarity = max(max_similarity, sim)
                    similarity = max_similarity
                else:
                    # 공통 키워드가 있으면 해당 키워드들의 평균 유사도 계산
                    similarities_sum = 0.0
                    for keyword in common_keywords:
                        q_vec = np.array(sbert_keywords[keyword])
                        a_vec = np.array(article_sbert_keywords[keyword])
                        sim = cosine_similarity([q_vec], [a_vec])[0][0]
                        similarities_sum += sim
                    similarity = similarities_sum / len(common_keywords)

                if similarity >= similarity_threshold:
                    # 키워드 정보 파싱
                    try:
                        tfidf_keywords_dict = json.loads(keywords_json) if keywords_json else {}
                        top_keywords = sorted(tfidf_keywords_dict.items(), key=lambda x: x[1], reverse=True)[:5]
                        keywords_str = ', '.join([word for word, _ in top_keywords])
                    except:
                        keywords_str = ''

                    similarities.append({
                        'article_id': article_id,
                        'title': title or '',
                        'summary': summary or '',
                        'similarity_score': float(similarity),
                        'keywords': keywords_str,
                        'common_keywords': list(common_keywords)
                    })
            except Exception as e:
                logger.warning(f"기사 {article_id} 유사도 계산 실패: {e}")
                continue

        # 유사도 기준으로 정렬하고 top_k 개 반환
        similarities.sort(key=lambda x: x['similarity_score'], reverse=True)
        return similarities[:top_k]

    loop = asyncio.get_event_loop()
    return await loop.run_in_executor(executor, _find_similar)

# --- 저장 통계 조회 ---
async def get_storage_statistics() -> dict:
    """저장된 벡터 데이터의 통계를 반환합니다."""
    def _get_stats():
        conn = get_db_connection()
        try:
            with conn.cursor() as cursor:
                # TF-IDF 벡터 통계
                cursor.execute("SELECT COUNT(*) FROM article_tfidf_vectors")
                tfidf_count = cursor.fetchone()[0]

                # SBERT 벡터 통계
                cursor.execute("SELECT COUNT(*) FROM article_semantic_vectors")
                sbert_count = cursor.fetchone()[0]

                # 전체 기사 수
                cursor.execute("SELECT COUNT(*) FROM articles WHERE summary IS NOT NULL AND summary != ''")
                total_articles = cursor.fetchone()[0]

                # 최근 처리된 기사들
                cursor.execute("""
                    SELECT COUNT(*) FROM article_semantic_vectors 
                    WHERE updated_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)
                """)
                recent_processed = cursor.fetchone()[0]

                # 샘플 데이터 조회
                cursor.execute("""
                    SELECT asv.article_id,
                           JSON_LENGTH(asv.vector) as sbert_keywords_count,
                           JSON_LENGTH(asv.keywords) as tfidf_keywords_count
                    FROM article_semantic_vectors asv
                    WHERE asv.vector IS NOT NULL AND asv.keywords IS NOT NULL
                    LIMIT 3
                """)
                samples = cursor.fetchall()

                return {
                    "total_articles": total_articles,
                    "tfidf_vectors_stored": tfidf_count,
                    "sbert_vectors_stored": sbert_count,
                    "processing_coverage": f"{(sbert_count/total_articles*100):.1f}%" if total_articles > 0 else "0%",
                    "recent_24h_processed": recent_processed,
                    "sample_data": [
                        {
                            "article_id": sample[0],
                            "sbert_keywords_count": sample[1],
                            "tfidf_keywords_count": sample[2]
                        } for sample in samples
                    ]
                }
        finally:
            conn.close()

    loop = asyncio.get_event_loop()
    return await loop.run_in_executor(executor, _get_stats)

# --- 특정 기사 벡터 조회 ---
async def get_article_vectors(article_id: int) -> dict:
    """특정 기사의 저장된 벡터들을 반환합니다."""
    def _get_vectors():
        conn = get_db_connection()
        try:
            with conn.cursor() as cursor:
                # TF-IDF 벡터 조회
                cursor.execute("""
                    SELECT vector FROM article_tfidf_vectors WHERE article_id = %s
                """, (article_id,))
                tfidf_result = cursor.fetchone()

                # SBERT 벡터 및 키워드 조회
                cursor.execute("""
                    SELECT vector, keywords FROM article_semantic_vectors WHERE article_id = %s
                """, (article_id,))
                sbert_result = cursor.fetchone()

                if not tfidf_result or not sbert_result:
                    return None

                # JSON 파싱
                tfidf_vector = json.loads(tfidf_result[0]) if tfidf_result[0] else []
                sbert_keywords = json.loads(sbert_result[0]) if sbert_result[0] else {}
                tfidf_keywords = json.loads(sbert_result[1]) if sbert_result[1] else {}

                return {
                    "article_id": article_id,
                    "tfidf_vector_length": len(tfidf_vector),
                    "tfidf_nonzero_count": sum(1 for x in tfidf_vector if x > 0),
                    "tfidf_keywords": tfidf_keywords,
                    "sbert_keywords": {
                        keyword: {
                            "vector_length": len(vector),
                            "vector_sample": vector[:5]  # 처음 5개 값만 샘플로
                        } for keyword, vector in sbert_keywords.items()
                    },
                    "stored_keywords_count": len(sbert_keywords)
                }
        finally:
            conn.close()

    loop = asyncio.get_event_loop()
    return await loop.run_in_executor(executor, _get_vectors)

# --- 서비스 상태 확인 ---
def is_service_ready() -> bool:
    """서비스가 준비되었는지 확인합니다."""
    return service_ready