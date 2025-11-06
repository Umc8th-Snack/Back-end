# main.py
from fastapi import FastAPI, HTTPException, status, Query
from fastapi.middleware.cors import CORSMiddleware
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

# --- FastAPI 애플리케이션 인스턴스 생성  ---
app = FastAPI(
    title="기사 NLP 마이크로서비스",
    description="기사 분석 및 추천을 위한 한국어 자연어 처리 마이크로서비스 (KeyBERT + SBERT)",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS 설정 추가
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 모든 origin 허용 (개발 환경용)
    allow_credentials=True,
    allow_methods=["*"],  # 모든 HTTP 메서드 허용
    allow_headers=["*"],  # 모든 헤더 허용
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
    articleId: Optional[int] = None
    action: str # "click" or "scrap"
    keyword: Optional[str] = None

class UserProfileRequest(BaseModel):
    userId: int
    interactions: List[UserInteraction]

class RecommendedArticle(BaseModel):
    articleId: int
    score: float

class FeedResponse(BaseModel):
    articles: List[RecommendedArticle]

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
        logger.info("DB 연결 풀 생성 성공")

        # 연결 테스트
        async with db_pool.acquire() as conn:
            async with conn.cursor() as cursor:
                await cursor.execute("SELECT 1")
                result = await cursor.fetchone()
                logger.info(f"DB 연결 테스트 성공: {result}")

        # NLP 모델 초기화
        if nlp_processor:
            await nlp_processor.initialize_nlp_service()
            logger.info("NLP 모델 로드 성공")
        else:
            logger.warning("NLP 모델이 로드되지 않았습니다.")

        logger.info("=" * 50)
        logger.info("서비스 준비 완료!")

    except Exception as e:
        logger.error(f"초기화 실패: {e}")
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

@app.get("/")
async def root():
    return {
        "service": "NLP Microservice",
        "version": "1.0.0",
        "status": "running"
    }

# --- 헬스체크 엔드포인트 ---
@app.get("/health")
async def health_check():
    return {"status": "ok"}

# --- 벡터화 엔드포인트 ---
@app.post("/api/nlp/vectorize/batch")
async def batch_vectorize_articles(
        limit: int = Query(50, description="한 번에 처리할 최대 기사 수"),
        force_update: bool = Query(False, description="기존 벡터 재생성 여부")
):
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

@app.post("/api/vectorize/articles")
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

                    if text.strip() and nlp_processor:
                        keywords_dict, vectors_dict = await nlp_processor.extract_keywords_and_vectors(text)
                    else:
                        keywords_dict, vectors_dict = {}, {}
                        if not text.strip():
                            logger.info(f"기사 {article_id}는 summary가 없어 빈 값으로 처리합니다.")

                    # 대표 벡터 계산
                    representative_vector = [0.0] * 768
                    if vectors_dict:
                        all_vectors = np.array(list(vectors_dict.values()))
                        avg_vector = np.mean(all_vectors, axis=0)
                        norm = np.linalg.norm(avg_vector)
                        if norm > 0:
                            representative_vector = (avg_vector / norm).tolist()

                    keywords_json_str = json.dumps(keywords_dict)
                    vector_json_str = json.dumps(vectors_dict)
                    rep_vector_str = json.dumps(representative_vector)

                    model_version = "keybert-multitask-v1"

                    await cursor.execute("""
                            INSERT INTO article_semantic_vectors (article_id, vector, keywords, representative_vector, model_version, created_at, updated_at)
                            VALUES (%s, %s, %s, %s, %s, NOW(), NOW())
                            ON DUPLICATE KEY UPDATE
                                vector = VALUES(vector),
                                keywords = VALUES(keywords),
                                representative_vector = VALUES(representative_vector),
                                model_version = VALUES(model_version),
                                updated_at = NOW()
                        """, (article_id, vector_json_str, keywords_json_str, rep_vector_str, model_version))

                    logger.info(f"기사 {article_id} (KeyBERT) 처리 완료")
                    processed_count += 1

                except Exception as e:
                    logger.error(f"기사 {article_id} 처리 중 오류: {e}", exc_info=True)
                    failed_ids.append(article_id)
                    continue
        return {"status": "completed", "total_requested": len(article_ids), "processed": processed_count, "failed": failed_ids}

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



@app.get("/api/articles/search")
async def search_articles_semantic(
        query: str = Query(..., description="검색할 단어"),
        page: int = Query(0, ge=0, description="페이지 번호"),
        size: int = Query(5, ge=1, le=50, description="페이지 크기"),
        threshold: float = Query(0.7, ge=0, le=1, description="최소 유사도 임계값")
):
    try:
        cleaned_query = query.strip()

        logger.info(f"검색 요청 - 검색어: '{cleaned_query}', 페이지: {page}, 크기: {size}")

        # 빈 검색어 체크
        if not cleaned_query:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="유효한 검색어를 입력해주세요."
            )

        if not db_pool:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="데이터베이스 연결이 없습니다."
            )

        # NLP 서비스 준비 상태 확인
        if not nlp_processor:
            logger.error("NLP 프로세서가 로드되지 않았습니다.")
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="NLP 서비스가 준비되지 않았습니다."
            )

        # 검색어 벡터화
        query_vector = await vectorize_raw_query(cleaned_query)
        if query_vector is None:
            logger.warning(f"검색어 벡터화 실패: {cleaned_query}")
            return SearchResponse(query=cleaned_query, totalCount=0, articles=[])

        async with db_pool.acquire() as conn:
            async with conn.cursor(aiomysql.DictCursor) as cursor:
                # 대표벡터를 사용한 빠른 검색
                await cursor.execute("""
                    SELECT a.article_id, a.title, a.summary, a.published_at, 
                           asv.representative_vector
                    FROM articles a 
                    INNER JOIN article_semantic_vectors asv ON a.article_id = asv.article_id
                    WHERE asv.representative_vector IS NOT NULL
                """)

                all_articles = await cursor.fetchall()
                search_results = []

                for article in all_articles:
                    try:
                        rep_vector = np.array(json.loads(article['representative_vector']))
                        similarity = cosine_similarity(query_vector, rep_vector)

                        if article['summary'] and cleaned_query in article['summary']:
                            similarity = 0.99

                        if similarity >= threshold:
                            search_results.append({
                                'article_id': article['article_id'],
                                'title': article['title'],
                                'summary': article['summary'],
                                'score': float(similarity),
                                'publishedAt': article['published_at'].isoformat() if article['published_at'] else None
                            })

                    except json.JSONDecodeError as e:
                        logger.warning(f"기사 {article['article_id']} JSON 파싱 오류: {e}")
                        continue
                    except Exception as e:
                        logger.warning(f"기사 {article['article_id']} 처리 중 오류: {e}")
                        continue

                # 정렬 및 페이징
                search_results.sort(key=lambda x: x['score'], reverse=True)
                paginated_results = search_results[page * size : (page + 1) * size]

                articles_response = [
                    ArticleSearchResult(**{k: v for k, v in res.items() if k != 'keywords'})
                    for res in paginated_results
                ]

                logger.info(f"검색 완료 - 전체: {len(search_results)}개, 반환: {len(articles_response)}개")

                return SearchResponse(
                    query=cleaned_query,
                    totalCount=len(search_results),
                    articles=articles_response
                )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"검색 중 오류 발생: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"검색 처리 중 오류가 발생했습니다: {str(e)}"
        )

# --- 헬퍼 함수 ---
async def calculate_weighted_average(
        sbert_vectors: dict,
        tfidf_scores: dict
) -> List[float]:
    if not sbert_vectors:
        return [0.0] * 768

    vectors = []
    weights = []

    for keyword in sbert_vectors:
        if keyword in tfidf_scores:
            vectors.append(sbert_vectors[keyword])
            weights.append(tfidf_scores[keyword])

    if not vectors:
        return [0.0] * 768

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

async def vectorize_raw_query(query: str) -> Optional[np.ndarray]:
    if not nlp_processor:
        logger.error("NLP 프로세서가 초기화되지 않았습니다.")
        return None

    try:
        vector = await nlp_processor.vectorize_query(query)
        return vector
    except Exception as e:
        logger.error(f"검색어 '{query}' 벡터 변환 실패: {e}")
        return None

def cosine_similarity(vec1: np.ndarray, vec2: np.ndarray) -> float:
    # 두 벡터 간의 코사인 유사도 계산
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
    # 키워드 문자열을 파싱하여 KeywordScore 리스트로 변환
    if not keywords_str:
        return []

    keywords = []
    try:
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

ACTION_WEIGHTS = {
    "scrap": 0.7,
    "click": 0.5,
    "search": 0.5
}

async def _get_representative_vectors(article_ids: List[int]) -> Dict[int, np.ndarray]:
    # 여러 기사의 대표 벡터를 한번에 조회
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
    TARGET_DIMENSION = 768

    # articleId가 있는 경우만 추출
    article_ids = [interaction.articleId for interaction in interactions if interaction.articleId]
    search_keywords = [inter.keyword for inter in interactions if inter.keyword]

    # 기사 벡터와 검색어 벡터를 모두 가져옴
    article_vectors_map = await _get_representative_vectors(article_ids)
    keyword_vectors_map = {}
    if search_keywords and nlp_processor:
        keyword_vectors_map = await nlp_processor.vectorize_keywords(search_keywords)

    if not article_vectors_map and not keyword_vectors_map:
        return None

    weighted_vectors = []
    total_weight = 0
    found_valid_vector = False

    for interaction in interactions:
        action = interaction.action
        weight = ACTION_WEIGHTS.get(action, 0.0)
        vector = None

        if interaction.keyword and interaction.keyword in keyword_vectors_map:
            vector = np.array(keyword_vectors_map[interaction.keyword])
        elif interaction.articleId and interaction.articleId in article_vectors_map:
            vector = article_vectors_map[interaction.articleId]

        if vector is not None:
            if vector.shape == (TARGET_DIMENSION,):
                weighted_vectors.append(vector * weight)
                total_weight += weight
                found_valid_vector = True
            else:
                logger.warning(
                    f"Skipping vector for action '{action}' (ID: {interaction.articleId}, Keyword: {interaction.keyword}). "
                    f"Expected dimension {TARGET_DIMENSION}, but got {vector.shape}."
                )
    if not found_valid_vector or not weighted_vectors or total_weight == 0:
        return None

    # 가중 평균 계산
    avg_vector = np.sum(weighted_vectors, axis=0) / total_weight
    norm = np.linalg.norm(avg_vector)
    return avg_vector / norm if norm > 0 else avg_vector

# --- 맞춤 피드 엔드포인트 ---
@app.post("/api/nlp/user-profile", status_code=status.HTTP_201_CREATED)
async def update_user_profile(request: UserProfileRequest):
    user_id = request.userId
    user_profile_vector = await _calculate_user_profile_vector(request.interactions)

    if user_profile_vector is None:
        raise HTTPException(status_code=400, detail="유효한 행동 로그가 없어 프로필을 생성할 수 없습니다.")

    vector_str = json.dumps(user_profile_vector.tolist())

    async with db_pool.acquire() as conn:
        async with conn.cursor() as cursor:
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
    # 맞춤 피드
    if not db_pool:
        raise HTTPException(status_code=503, detail="데이터베이스 연결이 없습니다.")

    async with db_pool.acquire() as conn:
        async with conn.cursor(aiomysql.DictCursor) as cursor:
            # 1. 사용자 벡터 조회
            await cursor.execute("SELECT vector FROM user_vectors WHERE user_id = %s", (user_id,))
            user_row = await cursor.fetchone()
            if not user_row or not user_row['vector']:
                raise HTTPException(status_code=404, detail=f"사용자 ID {user_id}의 프로필 벡터를 찾을 수 없습니다.")
            user_vector = np.array(json.loads(user_row['vector']))

            # 2. 모든 기사의 대표 벡터 조회 (NULL이 아닌 것만)
            await cursor.execute("SELECT article_id, representative_vector FROM article_semantic_vectors WHERE representative_vector IS NOT NULL")
            all_articles = await cursor.fetchall()

            # 3. 코사인 유사도 계산
            recommendations = []
            for article in all_articles:
                try:
                    if article['representative_vector']:
                        article_vector = np.array(json.loads(article['representative_vector']))
                        if user_vector.shape == article_vector.shape:
                            score = cosine_similarity(user_vector, article_vector)
                            recommendations.append(RecommendedArticle(articleId=article['article_id'], score=score))
                except Exception as e:
                    logger.warning(f"기사 ID {article.get('article_id')} 유사도 계산 중 오류: {e}")
                    continue

            recommendations.sort(key=lambda x: x.score, reverse=True)
            start_idx = page * size
            end_idx = start_idx + size

            return FeedResponse(articles=recommendations[start_idx:end_idx])

async def _get_search_keywords_for_user(user_id: int) -> List[str]:
    # 특정 사용자의 최근 검색어 목록을 DB에서 가져옴
    if not db_pool:
        return []

    async with db_pool.acquire() as conn:
        async with conn.cursor() as cursor:
            query = """
                SELECT keyword 
                FROM search_keywords 
                WHERE user_id = %s 
                ORDER BY created_at DESC 
                LIMIT 10
            """
            await cursor.execute(query, (user_id,))
            results = await cursor.fetchall()

            return [row[0] for row in results] if results else []

# --- 메인 실행 ---

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=5000, reload=True)