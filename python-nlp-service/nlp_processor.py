import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sentence_transformers import SentenceTransformer
from konlpy.tag import Okt
from typing import Dict, List, Tuple

# 전역 변수
tfidf_vectorizer = None
sbert_model = None
okt = None

async def initialize_nlp_service():
    """NLP 모델 초기화"""
    global tfidf_vectorizer, sbert_model, okt

    # 형태소 분석기
    okt = Okt()

    # TF-IDF 벡터라이저 (한국어 특화)
    tfidf_vectorizer = TfidfVectorizer(
        tokenizer=lambda x: okt.nouns(x),  # 명사만 추출
        max_features=1000,
        min_df=2,
        max_df=0.8
    )

    # SBERT 모델 (한국어 지원)
    sbert_model = SentenceTransformer('sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2')
    return True

async def extract_tfidf_keywords(text: str, top_k: int = 10) -> Dict[str, float]:
    global okt

    if not okt:
        okt = Okt()

    # 명사 추출
    nouns = okt.nouns(text)

    if not nouns:
        return {}

    # 단어 빈도 계산 (간단한 TF 계산)
    word_freq = {}
    for word in nouns:
        if len(word) >= 2:  # 2글자 이상만
            word_freq[word] = word_freq.get(word, 0) + 1

    # 빈도 기준 정렬
    sorted_words = sorted(word_freq.items(), key=lambda x: x[1], reverse=True)

    # 상위 k개 선택 및 정규화
    top_words = sorted_words[:top_k]
    if not top_words:
        return {}

    max_freq = top_words[0][1]

    # TF-IDF 점수 계산 (간단한 버전)
    result = {}
    for word, freq in top_words:
        # 정규화된 점수 (0-1 사이)
        score = freq / max_freq
        result[word] = score

    return result

async def generate_sbert_vectors(keywords: List[str]) -> Dict[str, List[float]]:
    global sbert_model

    if not sbert_model:
        sbert_model = SentenceTransformer('sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2')

    if not keywords:
        return {}

    # SBERT 임베딩 생성
    embeddings = sbert_model.encode(keywords)

    # 딕셔너리로 변환
    result = {}
    for keyword, embedding in zip(keywords, embeddings):
        result[keyword] = embedding.tolist()

    return result

async def vectorize_text(text: str) -> Tuple[Dict[str, float], Dict[str, List[float]]]:
    # TF-IDF로 상위 키워드 추출
    tfidf_keywords = await extract_tfidf_keywords(text, top_k=10)

    # 키워드들에 대한 SBERT 벡터 생성
    if tfidf_keywords:
        keywords_list = list(tfidf_keywords.keys())
        sbert_vectors = await generate_sbert_vectors(keywords_list)
    else:
        sbert_vectors = {}

    return tfidf_keywords, sbert_vectors

def is_service_ready() -> bool:
    """서비스 준비 상태 확인"""
    return sbert_model is not None and okt is not None

# 코사인 유사도 계산 (벡터 간)
def cosine_similarity(vec1: np.ndarray, vec2: np.ndarray) -> float:
    """두 벡터 간의 코사인 유사도 계산"""
    dot_product = np.dot(vec1, vec2)
    norm1 = np.linalg.norm(vec1)
    norm2 = np.linalg.norm(vec2)

    if norm1 == 0 or norm2 == 0:
        return 0.0

    return dot_product / (norm1 * norm2)