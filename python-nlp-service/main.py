# main.py
from fastapi import FastAPI, HTTPException, status, Query
from pydantic import BaseModel
import uvicorn
import asyncio
import logging
from typing import List, Dict, Any, Optional
import aiomysql
import os
from datetime import datetime
from dotenv import load_dotenv
import numpy as np

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
    """
    기사 ID 목록을 받아 DB에서 데이터를 조회하고 벡터화하여 저장합니다.

    article_semantic_vectors 테이블 구조:
    - article_id (PK)
    - vector
    - keywords
    - model_version
    - created_at
    - updated_at
    """
    if not article_ids:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="article_ids는 필수입니다."
        )

    if not db_pool:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="데이터베이스 연결이 없습니다."
        )

    async with db_pool.acquire() as conn:
        async with conn.cursor(aiomysql.DictCursor) as cursor:
            processed_count = 0
            failed_ids = []

            for article_id in article_ids:
                try:
                    # 1. DB에서 기사 정보 조회
                    await cursor.execute("""
                        SELECT article_id, title, summary 
                        FROM articles 
                        WHERE article_id = %s
                    """, (article_id,))

                    article = await cursor.fetchone()

                    if not article:
                        logger.warning(f"기사 ID {article_id}를 찾을 수 없습니다.")
                        failed_ids.append(article_id)
                        continue

                    # 텍스트 결합
                    text = f"{article['title']} {article['summary']}"

                    # 2. NLP 처리
                    if nlp_processor:
                        # TF-IDF 키워드 추출
                        tfidf_keywords = await nlp_processor.extract_tfidf_keywords(text, top_k=10)

                        # SBERT 벡터 생성
                        top_keywords = list(tfidf_keywords.keys())
                        sbert_vectors = await nlp_processor.generate_sbert_vectors(top_keywords)

                        # 가중 평균 벡터 계산
                        weighted_vector = await calculate_weighted_average(sbert_vectors, tfidf_keywords)
                    else:
                        # nlp_processor가 없으면 더미 데이터
                        tfidf_keywords = {"테스트": 1.0}
                        weighted_vector = [0.0] * 384

                    # 3. 벡터를 문자열로 변환
                    vector_str = ','.join(map(str, weighted_vector))
                    keywords_str = ','.join([f"{k}:{v:.4f}" for k, v in tfidf_keywords.items()])

                    # 4. 기존 벡터 확인 (article_id로 직접 확인)
                    await cursor.execute("""
                        SELECT article_id 
                        FROM article_semantic_vectors 
                        WHERE article_id = %s
                    """, (article_id,))

                    existing = await cursor.fetchone()

                    # 5. DB에 저장 또는 업데이트
                    if existing:
                        # 기존 레코드 업데이트
                        await cursor.execute("""
                            UPDATE article_semantic_vectors 
                            SET vector = %s, 
                                keywords = %s,
                                model_version = %s,
                                updated_at = NOW()
                            WHERE article_id = %s
                        """, (
                            f"[{vector_str}]",
                            keywords_str,
                            "tfidf-sbert-v1",
                            article_id
                        ))
                        logger.info(f"기사 {article_id} 벡터 업데이트 완료")
                    else:
                        # 새 레코드 삽입
                        await cursor.execute("""
                            INSERT INTO article_semantic_vectors 
                            (article_id, vector, keywords, model_version, created_at, updated_at)
                            VALUES (%s, %s, %s, %s, NOW(), NOW())
                        """, (
                            article_id,
                            f"[{vector_str}]",
                            keywords_str,
                            "tfidf-sbert-v1"
                        ))
                        logger.info(f"기사 {article_id} 벡터 신규 저장 완료")

                    processed_count += 1

                except Exception as e:
                    logger.error(f"기사 {article_id} 처리 중 오류: {e}")
                    failed_ids.append(article_id)
                    # 개별 트랜잭션 실패 시 롤백하지 않고 계속 진행
                    continue

            return {
                "status": "completed",
                "total_requested": len(article_ids),
                "processed": processed_count,
                "failed": failed_ids,
                "message": f"{processed_count}개 기사 벡터화 완료"
            }

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



# --- 메인 실행 ---

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=5000, reload=True)