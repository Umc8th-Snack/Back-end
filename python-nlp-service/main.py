# main.py
from fastapi import FastAPI, HTTPException, status, Query
from pydantic import BaseModel
import uvicorn
import asyncio
import logging
from typing import List, Dict, Any, Optional
import aiomysql
import os
from dotenv import load_dotenv
import numpy as np
import json

# .env 파일 로드
load_dotenv()

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# --- FastAPI 애플리케이션 인스턴스 생성 (이 부분이 누락되어 있었음!) ---
app = FastAPI(
    title="기사 NLP 마이크로서비스",
    description="기사 분석 및 추천을 위한 한국어 자연어 처리 마이크로서비스 (TF-IDF + SBERT)",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# DB 연결 설정 (.env에서 읽기)
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'port': int(os.getenv('DB_PORT', 3306)),
    'user': os.getenv('DB_USER', 'root'),
    'password': os.getenv('DB_PASSWORD', ''),
    'db': os.getenv('DB_NAME', 'snack_db'),
    'charset': os.getenv('DB_CHARSET', 'utf8mb4'),
    'autocommit': True
}

# DB 연결 풀 (전역 변수)
db_pool = None

# NLP 프로세서 import (없으면 임시 모듈)
try:
    import nlp_processor
except ImportError:
    logger.warning("nlp_processor 모듈을 찾을 수 없습니다. 기본 기능만 사용합니다.")
    nlp_processor = None

# --- Pydantic 모델 정의 ---

class ArticleVectorizeRequestDto(BaseModel):
    articleId: int
    title: str
    summary: str

class ArticleVectorizeListRequestDto(BaseModel):
    articles: List[ArticleVectorizeRequestDto]

class ArticleKeywordDto(BaseModel):
    word: str
    tfidf: float

class ArticleVectorizeResponseDto(BaseModel):
    articleId: int
    vector: List[float]
    keywords: List[ArticleKeywordDto]

class ArticleVectorizeListResponseDto(BaseModel):
    results: List[ArticleVectorizeResponseDto]

class ArticleDto(BaseModel):
    articleId: int
    title: str
    summary: str

class SearchArticleResponseDto(BaseModel):
    articles: List[ArticleDto]

# DTO 모델 정의
class KeywordScore(BaseModel):
    word: str
    tfidf: float

class ArticleSearchResult(BaseModel):
    article_id: int
    title: str
    summary: Optional[str] = None
    score: float
    # keywords: List[KeywordScore]
    publishedAt: Optional[str]

class SearchResponse(BaseModel):
    query: str
    totalCount: int
    articles: List[ArticleSearchResult]

class UserInteraction(BaseModel):
    articleId: int
    action: str # "click" or "scrap"

class UserProfileRequest(BaseModel):
    userId: int
    interactions: List[UserInteraction]

class RecommendedArticle(BaseModel):
    articleId: int
    score: float

class FeedResponse(BaseModel):
    articles: List[RecommendedArticle]

# --- 애플리케이션 이벤트 핸들러 ---

@app.on_event("startup")
async def startup_event():
    """애플리케이션 시작 시 실행"""
    global db_pool
    logger.info("=" * 50)
    logger.info("NLP 서비스 시작 중...")
    logger.info(f"DB 연결 설정: {DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['db']}")

    try:
        # DB 연결 풀 생성
        db_pool = await aiomysql.create_pool(
            **DB_CONFIG,
            minsize=5,
            maxsize=20
        )
        logger.info("✅ DB 연결 풀 생성 성공")

        # 연결 테스트
        async with db_pool.acquire() as conn:
            async with conn.cursor() as cursor:
                await cursor.execute("SELECT 1")
                result = await cursor.fetchone()
                logger.info(f"✅ DB 연결 테스트 성공: {result}")

        # NLP 모델 초기화
        if nlp_processor:
            await nlp_processor.initialize_nlp_service()
            logger.info("✅ NLP 모델 로드 성공")
        else:
            logger.warning("⚠️ NLP 모델이 로드되지 않았습니다.")

        logger.info("=" * 50)
        logger.info("🚀 서비스 준비 완료!")

    except Exception as e:
        logger.error(f"❌ 초기화 실패: {e}")
        logger.error("서버는 계속 실행되지만 기능이 제한될 수 있습니다.")

@app.on_event("shutdown")
async def shutdown_event():
    """애플리케이션 종료 시 실행"""
    global db_pool
    logger.info("서비스 종료 중...")

    if db_pool:
        db_pool.close()
        await db_pool.wait_closed()
        logger.info("DB 연결 풀 종료 완료")

# --- 헬스체크 엔드포인트 ---

@app.get("/")
async def root():
    """루트 엔드포인트"""
    return {
        "service": "NLP Microservice",
        "version": "1.0.0",
        "status": "running"
    }

@app.get("/health")
async def health_check():
    """서비스 상태 확인"""
    status = {
        "service": "healthy",
        "database": "unknown",
        "nlp_model": "unknown"
    }

    # DB 연결 확인
    if db_pool:
        try:
            async with db_pool.acquire() as conn:
                async with conn.cursor() as cursor:
                    await cursor.execute("SELECT 1")
                    status["database"] = "connected"
        except:
            status["database"] = "disconnected"

    # NLP 모델 확인
    if nlp_processor and hasattr(nlp_processor, 'is_service_ready'):
        status["nlp_model"] = "loaded" if nlp_processor.is_service_ready() else "not loaded"

    return status

# --- 벡터화 엔드포인트 ---
# main.py의 수정된 벡터화 함수 부분

@app.post("/api/nlp/vectorize/articles")
async def vectorize_articles_from_db(article_ids: List[int]):
    if not article_ids:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="article_ids는 필수입니다.")
    if not db_pool:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="데이터베이스 연결이 없습니다.")

    async with db_pool.acquire() as conn:
        async with conn.cursor(aiomysql.DictCursor) as cursor:
            processed_count, failed_ids = 0, []
            for article_id in article_ids:
                try:
                    await cursor.execute("SELECT article_id, title, summary FROM articles WHERE article_id = %s", (article_id,))
                    article = await cursor.fetchone()
                    if not article:
                        logger.warning(f"기사 ID {article_id}를 찾을 수 없습니다.")
                        failed_ids.append(article_id)
                        continue

                    text = article.get('summary') or ''

                    tfidf_keywords = {}
                    sbert_vectors = {}
                    representative_vector = [0.0] * 768

                    if not text.strip():
                        # summary가 비어있으면, 벡터와 키워드를 모두 빈 딕셔너리로 처리
                        logger.info(f"기사 {article_id}는 summary가 없어 빈 값으로 처리합니다.")
                    elif nlp_processor:
                        # summary가 있을 때만 NLP 처리
                        tfidf_keywords = await nlp_processor.extract_tfidf_keywords(text, top_k=10)
                        top_keywords = list(tfidf_keywords.keys())
                        if top_keywords:
                            sbert_vectors = await nlp_processor.generate_sbert_vectors(top_keywords)

                            # 대표 벡터 계산 (키워드 벡터들의 단순 평균)
                            if sbert_vectors:
                                all_vectors = np.array(list(sbert_vectors.values()))
                                avg_vector = np.mean(all_vectors, axis=0)
                                norm = np.linalg.norm(avg_vector)
                                if norm > 0:
                                    representative_vector = (avg_vector / norm).tolist()

                    vector_json_str = json.dumps(sbert_vectors)
                    keywords_json_str = json.dumps(tfidf_keywords)
                    rep_vector_str = json.dumps(representative_vector)

                    await cursor.execute("SELECT article_id FROM article_semantic_vectors WHERE article_id = %s", (article_id,))
                    existing = await cursor.fetchone()
                    if existing:
                        await cursor.execute(
                            "UPDATE article_semantic_vectors SET vector = %s, keywords = %s, representative_vector = %s, model_version = %s, updated_at = NOW() WHERE article_id = %s",
                            (vector_json_str, keywords_json_str, rep_vector_str, "sbert-keywords-v4", article_id)
                        )
                        logger.info(f"기사 {article_id} 키워드 및 대표 벡터 업데이트 완료")
                    else:
                        await cursor.execute(
                            "INSERT INTO article_semantic_vectors (article_id, vector, keywords, representative_vector, model_version, created_at, updated_at) VALUES (%s, %s, %s, %s, %s, NOW(), NOW())",
                            (article_id, vector_json_str, keywords_json_str, rep_vector_str, "sbert-keywords-v4")
                        )
                        logger.info(f"기사 {article_id} 키워드 및 대표 벡터 신규 저장 완료")

                    processed_count += 1

                except Exception as e:
                    logger.error(f"기사 {article_id} 처리 중 오류: {e}", exc_info=True)
                    failed_ids.append(article_id)
                    continue
        return {"status": "completed", "total_requested": len(article_ids), "processed": processed_count, "failed": failed_ids}

@app.post("/api/nlp/vectorize/batch")
async def batch_vectorize_articles(
        limit: int = Query(100, description="한 번에 처리할 최대 기사 수"),
        force_update: bool = Query(False, description="기존 벡터 재생성 여부")
):
    """
    벡터화되지 않은 기사들을 자동으로 찾아서 처리합니다.
    """

    if not db_pool:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="데이터베이스 연결이 없습니다."
        )

    async with db_pool.acquire() as conn:
        async with conn.cursor(aiomysql.DictCursor) as cursor:
            # 벡터화되지 않은 기사 찾기
            if force_update:
                # 강제 업데이트: 모든 기사 대상
                query = """
                    SELECT article_id 
                    FROM articles 
                    ORDER BY created_at DESC 
                    LIMIT %s
                """
            else:
                # 벡터가 없는 기사만 선택
                query = """
                    SELECT a.article_id 
                    FROM articles a
                    LEFT JOIN article_semantic_vectors asv 
                        ON a.article_id = asv.article_id
                    WHERE asv.article_id IS NULL
                    ORDER BY a.created_at DESC
                    LIMIT %s
                """

            await cursor.execute(query, (limit,))
            articles = await cursor.fetchall()

            if not articles:
                return {
                    "status": "no_articles",
                    "message": "처리할 기사가 없습니다."
                }

            article_ids = [a['article_id'] for a in articles]
            logger.info(f"벡터화할 기사 {len(article_ids)}개 발견: {article_ids[:10]}...")  # 처음 10개만 로그

            # 벡터화 수행
            return await vectorize_articles_from_db(article_ids)

# 테이블 스키마 확인 엔드포인트
@app.get("/api/db/check-schema")
async def check_db_schema():
    """데이터베이스 스키마와 데이터 상태를 확인합니다."""

    if not db_pool:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="데이터베이스 연결이 없습니다."
        )

    async with db_pool.acquire() as conn:
        async with conn.cursor(aiomysql.DictCursor) as cursor:
            result = {}

            try:
                # article_semantic_vectors 테이블 구조 확인
                await cursor.execute("""
                    SHOW COLUMNS FROM article_semantic_vectors
                """)
                result['article_semantic_vectors_columns'] = await cursor.fetchall()
            except Exception as e:
                result['article_semantic_vectors_error'] = str(e)

            try:
                # articles 테이블 구조 확인
                await cursor.execute("""
                    SHOW COLUMNS FROM articles
                """)
                result['articles_columns'] = await cursor.fetchall()
            except Exception as e:
                result['articles_error'] = str(e)

            try:
                # 테이블 데이터 개수 확인
                await cursor.execute("""
                    SELECT 
                        (SELECT COUNT(*) FROM articles) as article_count,
                        (SELECT COUNT(*) FROM article_semantic_vectors) as vector_count
                """)
                counts = await cursor.fetchone()
                result['data_counts'] = counts

                # 벡터화되지 않은 기사 개수
                await cursor.execute("""
                    SELECT COUNT(*) as unvectorized_count
                    FROM articles a
                    LEFT JOIN article_semantic_vectors asv 
                        ON a.article_id = asv.article_id
                    WHERE asv.article_id IS NULL
                """)
                unvectorized = await cursor.fetchone()
                result['unvectorized_articles'] = unvectorized['unvectorized_count']

            except Exception as e:
                result['count_error'] = str(e)

            return result

# 테스트용 엔드포인트 - 특정 기사의 벡터 확인
@app.get("/api/vectors/{article_id}")
async def get_article_vector(article_id: int):
    """특정 기사의 벡터 정보를 조회합니다."""

    if not db_pool:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="데이터베이스 연결이 없습니다."
        )

    async with db_pool.acquire() as conn:
        async with conn.cursor(aiomysql.DictCursor) as cursor:
            # 기사 정보 조회
            await cursor.execute("""
                SELECT a.article_id, a.title, a.summary,
                       asv.vector, asv.keywords, asv.model_version,
                       asv.created_at, asv.updated_at
                FROM articles a
                LEFT JOIN article_semantic_vectors asv 
                    ON a.article_id = asv.article_id
                WHERE a.article_id = %s
            """, (article_id,))

            result = await cursor.fetchone()

            if not result:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail=f"기사 ID {article_id}를 찾을 수 없습니다."
                )

            # 벡터가 있으면 길이 계산
            if result['vector']:
                vector_str = result['vector'].strip('[]')
                vector_length = len(vector_str.split(',')) if vector_str else 0
                result['vector_length'] = vector_length
                # 벡터 내용은 처음 10개만 표시
                vector_preview = ','.join(vector_str.split(',')[:10])
                result['vector_preview'] = f"[{vector_preview},...]"
            else:
                result['vector_length'] = 0
                result['vector_preview'] = None

            return result

# --- 검색 엔드포인트 ---
'''
@app.get("/api/articles/search/{keyword}")
async def search_articles_direct(
        keyword: str,
        page: int = Query(0, ge=0),
        size: int = Query(10, ge=1, le=50),
        threshold: float = Query(0.3, description="최소 유사도 임계값")
):
    """키워드를 벡터화하고 DB의 벡터들과 직접 비교하여 유사한 기사를 찾습니다."""

    if not keyword or not keyword.strip():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="keyword는 필수입니다."
        )

    # 간단한 구현 (실제로는 nlp_processor 사용)
    return SearchArticleResponseDto(articles=[])
'''
@app.get("/api/articles/search")
async def search_articles_semantic(
        query: str = Query(..., description="검색할 단어"),
        page: int = Query(0, ge=0, description="페이지 번호"),
        size: int = Query(10, ge=1, le=50, description="페이지 크기"),
        threshold: float = Query(0.3, ge=0, le=1, description="최소 유사도 임계값")
):
    """
    의미 기반 기사 검색 (키워드 벡터 직접 비교 방식)
    """
    if not db_pool:
        raise HTTPException(status_code=503, detail="데이터베이스 연결이 없습니다.")

    logger.info(f"🔍 의미 기반 검색 시작 - 검색어: '{query}'")
    query_vector = await vectorize_raw_query(query)
    if query_vector is None:
        return SearchResponse(query=query, totalCount=0, articles=[])

    async with db_pool.acquire() as conn:
        async with conn.cursor(aiomysql.DictCursor) as cursor:
            await cursor.execute(
                "SELECT a.article_id, a.title, a.summary, a.published_at, asv.vector, asv.keywords "
                "FROM articles a INNER JOIN article_semantic_vectors asv ON a.article_id = asv.article_id "
                "WHERE asv.vector IS NOT NULL AND JSON_LENGTH(asv.vector) > 0"
            )
            all_articles = await cursor.fetchall()
            if not all_articles:
                return SearchResponse(query=query, totalCount=0, articles=[])

            search_results = []
            for article in all_articles:
                try:
                    keyword_vectors = json.loads(article['vector'])
                    similarities = [cosine_similarity(query_vector, np.array(vec)) for vec in keyword_vectors.values()]
                    max_similarity = max(similarities) if similarities else 0.0

                    if max_similarity >= threshold:
                        search_results.append({
                            'article_id': article['article_id'],
                            'title': article['title'],
                            'summary': article['summary'],
                            'score': float(max_similarity),
                            'publishedAt': article['published_at'].isoformat() if article['published_at'] else None
                        })
                except Exception:
                    continue

            search_results.sort(key=lambda x: x['score'], reverse=True)
            paginated_results = search_results[page * size : (page + 1) * size]

            # 키워드는 반환하지 않으므로 ArticleSearchResult 생성 시 keywords 제외
            articles_response = [ArticleSearchResult(**{k: v for k, v in res.items() if k != 'keywords'}) for res in paginated_results]

            return SearchResponse(query=query, totalCount=len(search_results), articles=articles_response)

# --- 헬퍼 함수 ---

async def calculate_weighted_average(
        sbert_vectors: dict,
        tfidf_scores: dict
) -> List[float]:
    """TF-IDF 점수를 가중치로 사용하여 SBERT 벡터들의 가중 평균 계산"""

    if not sbert_vectors:
        return [0.0] * 384

    vectors = []
    weights = []

    for keyword in sbert_vectors:
        if keyword in tfidf_scores:
            vectors.append(sbert_vectors[keyword])
            weights.append(tfidf_scores[keyword])

    if not vectors:
        return [0.0] * 384

    # numpy 배열로 변환
    vectors = np.array(vectors)
    weights = np.array(weights)

    # 가중치 정규화
    weights = weights / weights.sum()

    # 가중 평균 계산
    weighted_avg = np.average(vectors, axis=0, weights=weights)

    # 벡터 정규화
    norm = np.linalg.norm(weighted_avg)
    if norm > 0:
        weighted_avg = weighted_avg / norm

    return weighted_avg.tolist()

@app.post("/api/nlp/search")
async def search_articles_semantic(
        query: str,
        page: int = Query(0, ge=0, description="페이지 번호"),
        size: int = Query(10, ge=1, le=50, description="페이지 크기"),
        threshold: float = Query(0.3, ge=0, le=1, description="최소 유사도 임계값")
):
    """
    의미 기반 기사 검색
    1. 검색어를 TF-IDF로 키워드 추출
    2. 키워드들의 SBERT 벡터 생성
    3. TF-IDF 가중 평균으로 쿼리 벡터 생성
    4. DB의 모든 기사 벡터와 코사인 유사도 계산
    5. 유사도 높은 순으로 정렬하여 반환
    """

    if not query or not query.strip():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="검색어를 입력해주세요."
        )

    if not db_pool:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="데이터베이스 연결이 없습니다."
        )

    logger.info(f"🔍 의미 기반 검색 시작 - 검색어: '{query}', 페이지: {page}, 크기: {size}")

    try:
        # 1. 검색어 벡터화
        query_vector = await vectorize_raw_query(query)

        if query_vector is None:
            logger.error("검색어 벡터화 실패")
            return SearchResponse(query=query, totalCount=0, articles=[])

        # 2. DB에서 모든 기사와 벡터 조회
        async with db_pool.acquire() as conn:
            async with conn.cursor(aiomysql.DictCursor) as cursor:
                # 모든 기사와 벡터 조회
                await cursor.execute("""
                    SELECT 
                        a.article_id,
                        a.title,
                        a.summary,
                        a.published_at,
                        asv.vector,
                        asv.keywords
                    FROM articles a
                    INNER JOIN article_semantic_vectors asv 
                        ON a.article_id = asv.article_id
                    WHERE asv.vector IS NOT NULL
                """)

                all_articles = await cursor.fetchall()

                if not all_articles:
                    logger.warning("벡터화된 기사가 없습니다.")
                    return SearchResponse(query=query, totalCount=0, articles=[])

                # 3. 코사인 유사도 계산 (새로운 로직)
                search_results = []
                for article in all_articles:
                    try:
                        # DB에 저장된 벡터(키워드별 벡터 JSON)와 키워드(TF-IDF 점수 JSON) 로드
                        keyword_vectors = json.loads(article['vector'] or '{}')
                        tfidf_scores = json.loads(article['keywords'] or '{}')

                        if not keyword_vectors:
                            continue

                        # 각 키워드 벡터와 검색어 벡터의 유사도 계산
                        similarities = [
                            cosine_similarity(query_vector, np.array(vec))
                            for vec in keyword_vectors.values()
                        ]

                        # 가장 높은 유사도 점수를 이 기사의 최종 점수로 결정
                        max_similarity = max(similarities) if similarities else 0.0

                        if max_similarity >= threshold:
                            # 키워드 DTO 생성 (이제 tfidf_scores 딕셔너리 사용)
                            keywords_dto = [
                                KeywordScore(word=w, tfidf=s)
                                for w, s in tfidf_scores.items()
                            ]

                            search_results.append({
                                'article_id': article['article_id'],
                                'title': article['title'],
                                'summary': article['summary'],
                                'score': float(max_similarity),
                                'keywords': keywords_dto,
                                'publishedAt': article['published_at'].isoformat() if article['published_at'] else None
                            })
                    except Exception as e:
                        logger.warning(f"기사 {article['article_id']} 검색 처리 중 오류: {e}")
                        continue


                # 4. 유사도 기준 내림차순 정렬
                search_results.sort(key=lambda x: x['score'], reverse=True)

                # 5. 페이징 처리
                total_count = len(search_results)
                start_idx = page * size
                end_idx = start_idx + size
                paginated_results = search_results[start_idx:end_idx]

                # 6. 응답 형식으로 변환
                articles = []
                for result in paginated_results:
                    articles.append(ArticleSearchResult(
                        article_id=result['article_id'],
                        title=result['title'],
                        summary=result['summary'],
                        score=result['score'],
                        keywords=result['keywords'],
                        publishedAt=result['publishedAt']
                    ))

                logger.info(f"✅ 검색 완료 - 전체: {total_count}개, 반환: {len(articles)}개")

                return SearchResponse(
                    query=query,
                    totalCount=total_count,
                    articles=articles
                )

    except Exception as e:
        logger.error(f"검색 중 오류 발생: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"검색 처리 중 오류가 발생했습니다: {str(e)}"
        )

async def vectorize_raw_query(query: str) -> Optional[np.ndarray]:
    """검색어 전체를 하나의 벡터로 변환"""
    if not nlp_processor:
        logger.error("NLP 프로세서가 초기화되지 않았습니다.")
        return None

    vector_dict = await nlp_processor.generate_sbert_vectors([query])
    if not vector_dict or query not in vector_dict:
        logger.error(f"검색어 '{query}'에 대한 SBERT 벡터 생성 실패")
        return None

    return np.array(vector_dict[query])

def cosine_similarity(vec1: np.ndarray, vec2: np.ndarray) -> float:
    """두 벡터 간의 코사인 유사도 계산"""
    try:
        # 벡터가 이미 정규화되어 있다면 단순 내적만 계산
        dot_product = np.dot(vec1, vec2)

        # 안전을 위해 norm 체크
        norm1 = np.linalg.norm(vec1)
        norm2 = np.linalg.norm(vec2)

        if norm1 == 0 or norm2 == 0:
            return 0.0

        similarity = dot_product / (norm1 * norm2)

        # 부동소수점 오차로 인한 범위 벗어남 방지
        return max(-1.0, min(1.0, similarity))

    except Exception as e:
        logger.error(f"코사인 유사도 계산 오류: {e}")
        return 0.0

def parse_keywords(keywords_str: str) -> List[KeywordScore]:
    """키워드 문자열을 파싱하여 KeywordScore 리스트로 변환"""
    if not keywords_str:
        return []

    keywords = []
    try:
        # "word1:0.5,word2:0.3" 형식 파싱
        pairs = keywords_str.split(',')
        for pair in pairs[:5]:  # 상위 5개만
            if ':' in pair:
                word, score = pair.split(':', 1)
                keywords.append(KeywordScore(
                    word=word.strip(),
                    tfidf=float(score)
                ))
    except Exception as e:
        logger.warning(f"키워드 파싱 오류: {e}")

    return keywords

# 테스트용 엔드포인트
@app.get("/api/test/search/{query}")
async def test_search(query: str):
    """간단한 검색 테스트"""
    try:
        result = await search_articles_semantic(query, page=0, size=5)
        return {
            "status": "success",
            "query": query,
            "found": result.totalCount,
            "top_results": [
                {
                    "id": article.article_id,
                    "title": article.title[:50],
                    "score": article.score
                }
                for article in result.articles
            ]
        }
    except Exception as e:
        return {
            "status": "error",
            "error": str(e)
        }

ACTION_WEIGHTS = {
    "click": 1.0,
    "scrap": 1.5,  # 스크랩에 더 높은 가중치 부여
}

async def _get_representative_vectors(article_ids: List[int]) -> Dict[int, np.ndarray]:
    """ 여러 기사의 대표 벡터를 한번에 조회 """
    if not article_ids or not db_pool:
        return {}

    vectors = {}
    query = f"SELECT article_id, representative_vector FROM article_semantic_vectors WHERE article_id IN ({','.join(['%s'] * len(article_ids))})"

    async with db_pool.acquire() as conn:
        async with conn.cursor(aiomysql.DictCursor) as cursor:
            await cursor.execute(query, tuple(article_ids))
            rows = await cursor.fetchall()
            for row in rows:
                if row['representative_vector']:
                    vectors[row['article_id']] = np.array(json.loads(row['representative_vector']))
    return vectors

async def _calculate_user_profile_vector(interactions: List[UserInteraction]) -> Optional[np.ndarray]:
    """ 행동 로그를 기반으로 사용자 프로필 벡터 계산 """
    article_ids = [interaction.articleId for interaction in interactions]
    article_vectors = await _get_representative_vectors(article_ids)

    if not article_vectors:
        return None

    weighted_vectors = []
    total_weight = 0

    for interaction in interactions:
        article_id = interaction.articleId
        action = interaction.action

        if article_id in article_vectors:
            vector = article_vectors[article_id]
            weight = ACTION_WEIGHTS.get(action, 0.0)

            weighted_vectors.append(vector * weight)
            total_weight += weight

    if not weighted_vectors:
        return None

    # 가중 평균 계산
    avg_vector = np.sum(weighted_vectors, axis=0) / total_weight
    norm = np.linalg.norm(avg_vector)
    if norm > 0:
        return avg_vector / norm
    return avg_vector


# --- 맞춤 피드 엔드포인트 ---

@app.post("/api/nlp/user-profile", status_code=status.HTTP_201_CREATED)
async def update_user_profile(request: UserProfileRequest):
    """
    사용자의 행동 로그를 받아 프로필 벡터를 생성/업데이트합니다.
    (Spring 백엔드에서 호출)
    """
    user_id = request.userId
    user_profile_vector = await _calculate_user_profile_vector(request.interactions)

    if user_profile_vector is None:
        raise HTTPException(status_code=400, detail="유효한 행동 로그가 없어 프로필을 생성할 수 없습니다.")

    vector_str = json.dumps(user_profile_vector.tolist())

    async with db_pool.acquire() as conn:
        async with conn.cursor() as cursor:
            # UPSERT (INSERT ... ON DUPLICATE KEY UPDATE)
            await cursor.execute("""
                INSERT INTO user_vectors (user_id, vector, model_version)
                VALUES (%s, %s, %s)
                ON DUPLICATE KEY UPDATE
                    vector = VALUES(vector),
                    model_version = VALUES(model_version),
                    updated_at = NOW()
            """, (user_id, vector_str, "profile-v1"))

    return {"userId": user_id, "status": "profile_updated"}

@app.get("/api/nlp/feed/{user_id}", response_model=FeedResponse)
async def get_personalized_feed(user_id: int, page: int = 0, size: int = 20):
    """
    저장된 사용자 프로필 벡터를 기반으로 맞춤 기사를 추천합니다.
    """
    async with db_pool.acquire() as conn:
        async with conn.cursor(aiomysql.DictCursor) as cursor:
            # 1. 사용자 벡터 조회
            await cursor.execute("SELECT vector FROM user_vectors WHERE user_id = %s", (user_id,))
            user_row = await cursor.fetchone()
            if not user_row or not user_row['vector']:
                raise HTTPException(status_code=404, detail="사용자 프로필 벡터를 찾을 수 없습니다. 프로필을 먼저 생성해주세요.")
            user_vector = np.array(json.loads(user_row['vector']))

            # 2. 모든 기사의 대표 벡터 조회
            await cursor.execute("SELECT article_id, representative_vector FROM article_semantic_vectors")
            all_articles = await cursor.fetchall()

            # 3. 코사인 유사도 계산
            recommendations = []
            for article in all_articles:
                if article['representative_vector']:
                    article_vector = np.array(json.loads(article['representative_vector']))
                    score = cosine_similarity(user_vector, article_vector)
                    recommendations.append(RecommendedArticle(articleId=article['article_id'], score=score))

            # 4. 유사도 순 정렬 및 페이징
            recommendations.sort(key=lambda x: x.score, reverse=True)
            start_idx = page * size
            end_idx = start_idx + size

            return FeedResponse(articles=recommendations[start_idx:end_idx])

# --- 메인 실행 ---

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=5000, reload=True)