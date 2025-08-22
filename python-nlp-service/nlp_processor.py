import numpy as np
import logging
from sentence_transformers import SentenceTransformer
from konlpy.tag import Okt
from typing import Dict, List, Tuple
from keybert import KeyBERT
import os
import pandas as pd

logger = logging.getLogger(__name__)

# data_source/stopwords.csv 가져오기
def _load_stopwords(file_path: str) -> set:
    default_stopwords = {
        '그', '저', '이', '것', '들', '의', '가', '을', '를', '에', '와', '과', '도', '만', '부터', '까지',
        '으로', '로', '에서', '에게', '한테', '께', '더', '덜', '가장', '매우', '아주', '정말', '진짜',
        '기자', '뉴스', '오늘', '어제', '내일', '오후', '오전', '시간', '때문', '통해', '대해', '관련',
        '지난', '현재', '앞으로', '계속', '계획', '발표', '보도', '내용', '결과', '상황', '문제', '방법',
        '경우', '때', '중', '후', '전', '동안', '사이', '위해', '따라', '따르면', '의하면', '밝혔다',
        '전했다', '말했다', '했다', '됐다', '있다', '없다', '이다', '아니다', '하다', '되다', '있습니다'
    }

    try:
        if os.path.exists(file_path):
            df = pd.read_csv(file_path, header=None)
            stopwords_set = set(df[0].tolist())
            logger.info(f"{len(stopwords_set)}개의 불용어를 '{file_path}'에서 로드했습니다.")
            return stopwords_set
        else:
            logger.warning(f"불용어 파일 '{file_path}'를 찾을 수 없습니다. 기본 불용어를 사용합니다.")
            return default_stopwords
    except Exception as e:
        logger.error(f"불용어 파일 로드 중 오류 발생: {e}. 기본 불용어를 사용합니다.")
        return default_stopwords

class NLPProcessor:
    # KeyBERT & SBERT 사용하여 키워드 추출 및 벡터화 처리
    def __init__(self, stopwords_path = "data_source/stopwords.csv"):
        self.keyword_model: SentenceTransformer = None  # 키워드 추출용
        self.vectorizer_model: SentenceTransformer = None # 벡터 변환용
        self.keybert_model: KeyBERT = None
        self.okt: Okt = None
        self.stopwords: set = _load_stopwords(stopwords_path)

    async def initialize_nlp_service(self):
        if self.keyword_model is None or self.vectorizer_model is None:
            # 1. 키워드 추출용 모델 로드
            kw_model_name = 'jhgan/ko-sroberta-multitask'
            logger.info(f"Loading Keyword-Extractor Model: {kw_model_name}...")
            self.keyword_model = SentenceTransformer(kw_model_name)
            self.keybert_model = KeyBERT(self.keyword_model)

            # 2. 의미 벡터 변환용 모델 로드
            vec_model_name = 'jhgan/ko-sbert-sts'
            logger.info(f"Loading Vectorizer Model: {vec_model_name}...")
            self.vectorizer_model = SentenceTransformer(vec_model_name)

            self.okt = Okt()
            logger.info("모든 NLP 모델이 초기화되었습니다.")
    async def extract_keywords_and_vectors(self, text: str, top_k: int = 10) -> Tuple[Dict[str, float], Dict[str, List[float]]]:
        # 텍스트 -> 벡터 변환

        if not self.keybert_model or not self.vectorizer_model:
            raise RuntimeError("NLP models are not initialized.")

        try:
            keywords_with_scores = self.keybert_model.extract_keywords(
                text, keyphrase_ngram_range=(1, 2), stop_words=list(self.stopwords),
                top_n=top_k, use_mmr=True, diversity=0.7
            )

            if not keywords_with_scores:
                return {}, {}

            keywords_dict = {kw: float(score) for kw, score in keywords_with_scores}
            keywords_list = list(keywords_dict.keys())

            embeddings = self.vectorizer_model.encode(keywords_list)
            vectors_dict = {kw: emb.tolist() for kw, emb in zip(keywords_list, embeddings)}

            return keywords_dict, vectors_dict

        except Exception as e:
            logger.error(f"벡터화 중 오류가 발생했습니다.: {e}", exc_info=True)
            return {}, {}

processor = NLPProcessor()

async def initialize_nlp_service():
    await processor.initialize_nlp_service()

async def extract_keywords_and_vectors(text: str, top_k: int = 10):
    return await processor.extract_keywords_and_vectors(text, top_k)

def cosine_similarity(vec1: np.ndarray, vec2: np.ndarray) -> float:
    # 두 벡터 간의 코사인 유사도 계산
    dot_product = np.dot(vec1, vec2)
    norm1 = np.linalg.norm(vec1)
    norm2 = np.linalg.norm(vec2)

    if norm1 == 0 or norm2 == 0:
        return 0.0

    return dot_product / (norm1 * norm2)

async def vectorize_query(query: str) -> np.ndarray:
    """사용자 검색어를 의미 벡터로 변환합니다."""
    if not processor.vectorizer_model:
        raise RuntimeError("Vectorizer model is not initialized.")

    embedding = processor.vectorizer_model.encode(query)

    return embedding

async def vectorize_keywords(keywords: List[str]) -> Dict[str, List[float]]:
    # 키워드 '리스트'를 입력받아 벡터 '딕셔너리'를 반환
    if not processor.vectorizer_model:
        raise RuntimeError("Vectorizer model is not initialized.")
    if not keywords:
        return {}

    try:
        embeddings = processor.vectorizer_model.encode(keywords)
        return {keyword: emb.tolist() for keyword, emb in zip(keywords, embeddings)}
    except Exception as e:
        logger.error(f"키워드 리스트 벡터 변환 중 오류: {e}")
        return {}