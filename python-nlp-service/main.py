# main.py
from fastapi import FastAPI, HTTPException, status, Query
from pydantic import BaseModel
import uvicorn
import nlp_processor
import asyncio
import logging
from typing import List, Dict, Any, Optional

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# --- FastAPI 애플리케이션 인스턴스 생성 ---
app = FastAPI(
    title="기사 NLP 마이크로서비스",
    description="기사 분석 및 추천을 위한 한국어 자연어 처리 마이크로서비스 (TF-IDF + SBERT)",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# --- 애플리케이션 시작 시 실행되는 이벤트 핸들러 ---
@app.on_event("startup")
async def startup_event():
    logger.info("NLP 모델 및 데이터 로딩 시작...")
    try:
        await nlp_processor.initialize_nlp_service()
        logger.info("NLP 모델 및 데이터 로드 성공.")
    except Exception as e:
        logger.error(f"시작 시 NLP 리소스 로드 실패: {e}")
        logger.warning("서버는 계속 실행되지만 NLP 기능이 제한될 수 있습니다.")

# --- Spring DTO와 일치하는 Pydantic 모델 정의 ---

# 1. ArticleVectorizeRequestDto
class ArticleVectorizeRequestDto(BaseModel):
    articleId: int
    title: str
    summary: str

# 2. ArticleVectorizeListRequestDto
class ArticleVectorizeListRequestDto(BaseModel):
    articles: List[ArticleVectorizeRequestDto]

# 3. ArticleKeywordDto
class ArticleKeywordDto(BaseModel):
    word: str
    tfidf: float

# 4. ArticleVectorizeResponseDto
class ArticleVectorizeResponseDto(BaseModel):
    articleId: int
    vector: List[float]  # double[] -> List[float]로 변환
    keywords: List[ArticleKeywordDto]

# 5. ArticleVectorizeListResponseDto
class ArticleVectorizeListResponseDto(BaseModel):
    results: List[ArticleVectorizeResponseDto]

# 6. QueryVectorizeRequestDto
class QueryVectorizeRequestDto(BaseModel):
    query: str

# 7. QueryVectorizeResponseDto
class QueryVectorizeResponseDto(BaseModel):
    query: str
    vector: List[float]  # double[] -> List[float]로 변환

# 8. SearchArticleResponseDto (Article 엔티티 대신 간단한 구조)
class ArticleDto(BaseModel):
    articleId: int
    title: str
    summary: str
    # 필요한 다른 Article 필드들 추가 가능

class SearchArticleResponseDto(BaseModel):
    articles: List[ArticleDto]

# --- Spring 컨트롤러와 완전히 일치하는 API 엔드포인트 ---

@app.get("/api/articles/search/{keyword}",
         response_model=SearchArticleResponseDto,
         status_code=status.HTTP_200_OK,
         summary="기사 검색 (로컬 TF-IDF)")
async def search_articles_local(
        keyword: str,
        sort: str = Query("relevance", description="정렬 방식 (latest, relevance)"),
        page: int = Query(0, ge=0, description="페이지 번호 (0부터 시작)"),
        size: int = Query(10, ge=1, le=50, description="페이지 크기")):
    """
    검색어를 입력하여 관련 기사를 로컬 TF-IDF 벡터 유사도 기반으로 검색합니다.
    """
    if not keyword or not keyword.strip():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="keyword는 필수이며 비어있을 수 없습니다."
        )

    try:
        logger.info(f"기사 검색 요청 - 키워드: {keyword}, 정렬: {sort}, 페이지: {page}, 크기: {size}")

        # FastAPI의 기존 search 로직 활용
        similar_articles = await nlp_processor.find_similar_articles(
            keyword,
            top_k=size * (page + 2),
            similarity_threshold=0.3
        )

        # 페이징 처리
        start_idx = page * size
        end_idx = start_idx + size
        paginated_articles = similar_articles[start_idx:end_idx]

        # ArticleDto 형태로 변환
        article_dtos = []
        for article in paginated_articles:
            article_dto = ArticleDto(
                articleId=article.get("article_id", 0),
                title=article.get("title", ""),
                summary=article.get("summary", "")
            )
            article_dtos.append(article_dto)

        return SearchArticleResponseDto(articles=article_dtos)

    except Exception as e:
        logger.error(f"기사 검색 중 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="기사 검색 중 오류가 발생했습니다."
        )

@app.get("/api/articles/search-fastapi/{keyword}",
         response_model=SearchArticleResponseDto,
         status_code=status.HTTP_200_OK,
         summary="기사 검색 (FastAPI)")
async def search_articles_fastapi_backup(
        keyword: str,
        page: int = Query(0, ge=0, description="페이지 번호 (0부터 시작)"),
        size: int = Query(10, ge=1, le=50, description="페이지 크기")):
    """
    FastAPI를 통한 기사 검색 (백업용)
    """
    logger.info(f"FastAPI 기사 검색 요청 - 키워드: {keyword}, 페이지: {page}, 크기: {size}")

    # 동일한 로직 사용
    return await search_articles_local(keyword, "relevance", page, size)

@app.get("/api/articles/search/test",
         response_model=SearchArticleResponseDto,
         status_code=status.HTTP_200_OK,
         summary="검색 테스트")
async def test_search(query: str = Query("기술", description="테스트 검색어")):
    """
    간단한 로컬 검색 기능을 테스트합니다.
    """
    logger.info(f"검색 테스트 - 검색어: {query}")

    try:
        # 고정된 파라미터로 테스트
        return await search_articles_local(query, "relevance", 0, 5)
    except Exception as e:
        logger.error(f"검색 테스트 중 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="검색 테스트 중 오류가 발생했습니다."
        )

# --- NLP 관련 엔드포인트 (벡터화 등) ---

@app.post("/api/nlp/vectorize/articles",
          response_model=ArticleVectorizeListResponseDto,
          status_code=status.HTTP_201_CREATED,
          summary="기사 벡터화")
async def vectorize_articles_api(request_data: ArticleVectorizeListRequestDto):
    """
    새로 수집된 기사들을 받아 본문을 분석하고 의미 벡터를 생성하여 DB에 저장합니다. (내부용)
    """
    if not request_data.articles:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="articles 필드는 필수이며 비어있을 수 없습니다."
        )

    try:
        results = []

        for article in request_data.articles:
            # 각 기사에 대해 벡터화 수행
            tfidf_keywords, sbert_keywords = await nlp_processor.vectorize_text(article.summary)

            # 벡터 저장
            await nlp_processor.save_article_vectors(
                article.articleId,
                article.title,
                article.summary,
                tfidf_keywords,
                sbert_keywords
            )

            # 키워드를 ArticleKeywordDto 형태로 변환
            keyword_dtos = [
                ArticleKeywordDto(word=word, tfidf=score)
                for word, score in tfidf_keywords.items()
            ]

            # 의미벡터를 List[float]로 변환
            vector = []
            if sbert_keywords:
                first_keyword_vector = next(iter(sbert_keywords.values()))
                vector = first_keyword_vector if first_keyword_vector else []

            # 응답 DTO 생성
            response_dto = ArticleVectorizeResponseDto(
                articleId=article.articleId,
                vector=vector,
                keywords=keyword_dtos
            )
            results.append(response_dto)

        return ArticleVectorizeListResponseDto(results=results)

    except Exception as e:
        logger.error(f"기사 벡터화 중 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="기사 벡터화 중 오류가 발생했습니다."
        )

@app.post("/api/nlp/vectorize/query",
          response_model=QueryVectorizeResponseDto,
          status_code=status.HTTP_200_OK,
          summary="검색 쿼리 벡터화")
async def vectorize_query_api(request_data: QueryVectorizeRequestDto):
    """
    사용자의 검색 쿼리를 받아 벡터화한 결과를 반환합니다. (내부용)
    """
    if not request_data.query or not request_data.query.strip():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="query 필드는 필수이며 비어있을 수 없습니다."
        )

    try:
        tfidf_keywords, sbert_keywords = await nlp_processor.vectorize_text(request_data.query)

        # 의미벡터를 List[float]로 변환 (키워드들의 평균 벡터 계산)
        vector = []
        if sbert_keywords:
            all_vectors = list(sbert_keywords.values())
            if all_vectors:
                vector_length = len(all_vectors[0])
                vector = [
                    sum(vec[i] for vec in all_vectors) / len(all_vectors)
                    for i in range(vector_length)
                ]

        return QueryVectorizeResponseDto(
            query=request_data.query,
            vector=vector
        )

    except RuntimeError as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"NLP 서비스 준비 안됨: {e}"
        )
    except Exception as e:
        logger.error(f"검색어 벡터화 중 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="검색어 벡터화 중 오류가 발생했습니다."
        )

@app.post("/api/nlp/admin/process-all-articles",
          status_code=status.HTTP_200_OK,
          summary="모든 기사 벡터화 처리")
async def process_all_articles_api(
        reprocess: bool = Query(False, description="기존 벡터를 다시 처리할지 여부")):
    """
    DB의 모든 기사를 FastAPI를 통해 벡터화합니다. (관리자용)
    """
    try:
        processed_count, total_count = await nlp_processor.process_all_articles(reprocess)

        result = {
            "message": "전체 기사 벡터화 처리 완료",
            "processed_count": processed_count,
            "total_count": total_count
        }

        return result

    except Exception as e:
        logger.error(f"전체 기사 벡터화 처리 중 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="전체 기사 벡터화 처리 중 오류가 발생했습니다."
        )

@app.post("/api/nlp/test/vectorize",
          response_model=QueryVectorizeResponseDto,
          status_code=status.HTTP_200_OK,
          summary="단일 텍스트 벡터화 테스트")
async def test_vectorize_api(request_data: QueryVectorizeRequestDto):
    """
    단일 텍스트를 벡터화하여 결과를 확인합니다. (테스트용)
    """
    if not request_data.query or not request_data.query.strip():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="query 필드는 필수이며 비어있을 수 없습니다."
        )

    try:
        tfidf_keywords, sbert_keywords = await nlp_processor.vectorize_text(request_data.query)

        # 의미벡터를 List[float]로 변환
        vector = []
        if sbert_keywords:
            all_vectors = list(sbert_keywords.values())
            if all_vectors:
                vector_length = len(all_vectors[0])
                vector = [
                    sum(vec[i] for vec in all_vectors) / len(all_vectors)
                    for i in range(vector_length)
                ]

        return QueryVectorizeResponseDto(
            query=request_data.query,
            vector=vector
        )
    except Exception as e:
        logger.error(f"텍스트 벡터화 테스트 중 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="텍스트 벡터화 테스트 중 오류가 발생했습니다."
        )

# --- 상태 확인 및 통계 엔드포인트 ---
@app.get("/health", status_code=status.HTTP_200_OK, summary="서비스 상태 확인")
async def health_check():
    """서비스 상태를 확인합니다."""
    try:
        is_ready = nlp_processor.is_service_ready()
        return {
            "status": "healthy" if is_ready else "initializing",
            "message": "NLP 서비스가 정상 작동 중입니다." if is_ready else "NLP 서비스 초기화 중입니다."
        }
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"서비스 상태 확인 실패: {e}"
        )

@app.get("/storage-stats", status_code=status.HTTP_200_OK, summary="벡터 저장 통계 확인")
async def get_storage_stats():
    """저장된 벡터 데이터의 통계를 확인합니다."""
    try:
        stats = await nlp_processor.get_storage_statistics()
        return stats
    except Exception as e:
        logger.error(f"저장 통계 조회 중 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="저장 통계 조회 중 오류가 발생했습니다."
        )

@app.get("/article-vectors/{article_id}", status_code=status.HTTP_200_OK, summary="특정 기사 벡터 조회")
async def get_article_vectors(article_id: int):
    """특정 기사의 저장된 벡터들을 조회합니다."""
    try:
        vectors = await nlp_processor.get_article_vectors(article_id)
        if not vectors:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"기사 ID {article_id}의 벡터를 찾을 수 없습니다."
            )
        return vectors
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"기사 벡터 조회 중 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="기사 벡터 조회 중 오류가 발생했습니다."
        )

# --- 기존 호환성을 위한 엔드포인트 (필요시 유지) ---
@app.post("/vectorize-text", response_model=QueryVectorizeResponseDto, status_code=status.HTTP_200_OK,
          summary="텍스트 벡터화 (기존 호환성용)")
async def vectorize_text_legacy(request_data: QueryVectorizeRequestDto):
    """기존 호환성을 위한 텍스트 벡터화 엔드포인트"""
    return await vectorize_query_api(request_data)

@app.post("/find-similar-articles", status_code=status.HTTP_200_OK,
          summary="유사 기사 검색 (기존 호환성용)")
async def find_similar_articles_legacy(request_data: dict):
    """기존 호환성을 위한 유사 기사 검색 엔드포인트"""
    query_text = request_data.get("query_text", "")
    top_k = request_data.get("top_k", 5)

    return await search_articles_api(query=query_text, page=0, size=top_k)

@app.post("/process-all-articles", status_code=status.HTTP_200_OK,
          summary="모든 기사 벡터화 처리 (기존 호환성용)")
async def process_all_articles_legacy(request_data: dict):
    """기존 호환성을 위한 전체 기사 처리 엔드포인트"""
    reprocess = request_data.get("reprocess", False)
    return await process_all_articles_api(reprocess=reprocess)

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=5000, reload=True)