import numpy as np
from sentence_transformers import SentenceTransformer
from konlpy.tag import Okt
from typing import Dict, List, Tuple

# 전역 변수
sbert_model = None
okt = None

async def initialize_nlp_service():
    """NLP 모델 초기화"""
    global sbert_model, okt

    okt = Okt()

    sbert_model = SentenceTransformer('jhgan/ko-sbert-sts')
    print("✅ NLP Service Initialized with jhgan/ko-sbert-sts")

    return True

async def extract_tf_keywords(text: str, top_k: int = 10) -> Dict[str, float]:
    global okt
    if not okt:
        okt = Okt()

    nouns = okt.nouns(text)
    if not nouns:
        return {}

    word_freq = {}
    for word in nouns:
        if len(word) >= 2:
            word_freq[word] = word_freq.get(word, 0) + 1

    sorted_words = sorted(word_freq.items(), key=lambda x: x[1], reverse=True)
    top_words = sorted_words[:top_k]
    if not top_words:
        return {}

    max_freq = top_words[0][1]
    result = {word: freq / max_freq for word, freq in top_words}
    return result

async def generate_sbert_vectors(keywords: List[str]) -> Dict[str, List[float]]:
    global sbert_model

    if not sbert_model:
        raise RuntimeError("SBERT model is not initialized. Please run initialize_nlp_service() first.")

    if not keywords:
        return {}

    embeddings = sbert_model.encode(keywords)
    result = {keyword: embedding.tolist() for keyword, embedding in zip(keywords, embeddings)}
    return result

async def vectorize_text(text: str) -> Tuple[Dict[str, float], Dict[str, List[float]]]:
    tf_keywords = await extract_tf_keywords(text, top_k=10)

    sbert_vectors = {}
    if tf_keywords:
        keywords_list = list(tf_keywords.keys())
        sbert_vectors = await generate_sbert_vectors(keywords_list)

    return tf_keywords, sbert_vectors

def is_service_ready() -> bool:
    """서비스 준비 상태 확인"""
    return sbert_model is not None and okt is not None

def cosine_similarity(vec1: np.ndarray, vec2: np.ndarray) -> float:
    """두 벡터 간의 코사인 유사도 계산"""
    dot_product = np.dot(vec1, vec2)
    norm1 = np.linalg.norm(vec1)
    norm2 = np.linalg.norm(vec2)

    if norm1 == 0 or norm2 == 0:
        return 0.0

    return dot_product / (norm1 * norm2)