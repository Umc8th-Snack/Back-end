# config.py
import os
from dotenv import load_dotenv

# .env 파일에서 환경 변수를 로드합니다.
# 이 파일을 실행하는 스크립트의 위치에 따라 .env 파일의 경로가 달라질 수 있습니다.
# 여기서는 nlp_processor.py나 main.py에서 로드될 때를 가정합니다.
load_dotenv()

# --- TF-IDF 모델 파일 경로 ---
# FastAPI 서비스가 실행되는 경로 기준 (예: python-nlp-service/data/)
# os.path.dirname(__file__)는 현재 파일(config.py)의 디렉토리를 반환합니다.
TFIDF_MODEL_PATH = os.path.join(os.path.dirname(__file__), 'data', 'tfidf_vectorizer.pkl')

# --- 데이터베이스 연결 설정 (MySQL) ---
# .env 파일에서 환경 변수를 읽어옵니다. 환경 변수가 없으면 기본값을 사용합니다.
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_PORT = int(os.getenv('DB_PORT', 3306))
DB_USER = os.getenv('DB_USER', 'root')
DB_PASSWORD = os.getenv('DB_PASSWORD', 'your_password') # 개발 환경 기본값, 실제로는 .env에서 로드
DB_NAME = os.getenv('DB_NAME', 'your_database') # 개발 환경 기본값, 실제로는 .env에서 로드
