# main.py
from fastapi import FastAPI, HTTPException, status
from pydantic import BaseModel
import uvicorn
import nlp_processor
import asyncio
import logging

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
        # 개발 중에는 에러가 나도 서버를 시작하도록 함
        logger.warning("서버는 계속 실행되지만 NLP 기능이 제한될 수 있습니다.")

# --- Pydantic 모델 정의 ---
class TextVectorRequest(BaseModel):
    text: str

class VectorResponse(BaseModel):
    tfidf_keywords: dict[str, float]  # 키워드: TF-IDF 점수 딕셔너리
    sbert_keywords: dict[str, list[float]]  # 키워드: SBERT 벡터 딕셔너리

class ProcessAllArticlesRequest(BaseModel):
    reprocess: bool = False  # 기존 벡터를 다시 처리할지 여부

class ProcessAllArticlesResponse(BaseModel):
    message: str
    processed_count: int
    total_count: int

class SimilarArticlesRequest(BaseModel):
    query_text: str
    top_k: int = 5
    similarity_threshold: float = 0.3

class SimilarArticle(BaseModel):
    article_id: int
    title: str
    summary: str
    similarity_score: float
    keywords: str = ""  # TF-IDF 기반 추출된 키워드들

class SimilarArticlesResponse(BaseModel):
    query_text: str
    similar_articles: list[SimilarArticle]

# --- API 엔드포인트 ---
@app.post("/vectorize-text", response_model=VectorResponse, status_code=status.HTTP_200_OK,
          summary="텍스트 벡터화 (TF-IDF 키워드 + SBERT 키워드별 벡터)")
async def vectorize_text_api(request_data: TextVectorRequest):
    """
    제공된 텍스트를 TF-IDF 키워드와 키워드별 SBERT 벡터로 변환합니다.
    - **text**: 벡터화할 텍스트 (필수)
    - 반환: TF-IDF 키워드 딕셔너리와 키워드별 SBERT 벡터 딕셔너리
    """
    text = request_data.text
    if not text or not text.strip():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="'text' 필드는 필수이며 비어있을 수 없습니다."
        )

    try:
        tfidf_keywords, sbert_keywords = await nlp_processor.vectorize_text(text)
        return {
            "tfidf_keywords": tfidf_keywords,
            "sbert_keywords": sbert_keywords
        }
    except RuntimeError as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"NLP 서비스 준비 안됨: {e}"
        )
    except Exception as e:
        logger.error(f"텍스트 벡터화 중 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="텍스트 벡터화 중 오류가 발생했습니다."
        )

@app.post("/process-all-articles", response_model=ProcessAllArticlesResponse,
          status_code=status.HTTP_200_OK, summary="모든 기사 벡터화 처리")
async def process_all_articles_api(request_data: ProcessAllArticlesRequest):
    """
    DB의 모든 articles의 summary를 벡터화하여 저장합니다.
    - **reprocess**: 기존에 처리된 기사도 다시 처리할지 여부 (기본값: False)
    - 반환: 처리 결과 및 통계
    """
    try:
        processed_count, total_count = await nlp_processor.process_all_articles(request_data.reprocess)
        return {
            "message": "기사 벡터화 처리 완료",
            "processed_count": processed_count,
            "total_count": total_count
        }
    except Exception as e:
        logger.error(f"기사 벡터화 처리 중 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="기사 벡터화 처리 중 오류가 발생했습니다."
        )

@app.post("/find-similar-articles", response_model=SimilarArticlesResponse,
          status_code=status.HTTP_200_OK, summary="유사 기사 검색")
async def find_similar_articles_api(request_data: SimilarArticlesRequest):
    """
    입력된 텍스트와 유사한 기사들을 찾습니다.
    - **query_text**: 검색할 텍스트
    - **top_k**: 반환할 유사 기사 개수 (기본값: 5)
    - **similarity_threshold**: 유사도 임계값 (기본값: 0.3)
    """
    if not request_data.query_text or not request_data.query_text.strip():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="query_text는 필수이며 비어있을 수 없습니다."
        )

    try:
        similar_articles = await nlp_processor.find_similar_articles(
            request_data.query_text,
            request_data.top_k,
            request_data.similarity_threshold
        )
        return {
            "query_text": request_data.query_text,
            "similar_articles": similar_articles
        }
    except Exception as e:
        logger.error(f"유사 기사 검색 중 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="유사 기사 검색 중 오류가 발생했습니다."
        )

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

# --- 기존 호환성을 위한 엔드포인트 ---
@app.post("/vectorize-tfidf", response_model=dict, status_code=status.HTTP_200_OK,
          summary="텍스트 TF-IDF 키워드 추출 (호환성용)")
async def vectorize_tfidf_api(request_data: TextVectorRequest):
    """
    기존 호환성을 위한 TF-IDF 키워드 추출 엔드포인트
    """
    if not nlp_processor.is_service_ready():
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="NLP 서비스가 아직 준비되지 않았습니다."
        )

    text = request_data.text
    if not text or not text.strip():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="'text' 필드는 필수이며 비어있을 수 없습니다."
        )

    try:
        tfidf_keywords, _ = await nlp_processor.vectorize_text(text)
        return {"keywords": tfidf_keywords}
    except Exception as e:
        logger.error(f"텍스트 벡터화 중 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="텍스트 벡터화 중 오류가 발생했습니다."
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

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=9000, reload=True)