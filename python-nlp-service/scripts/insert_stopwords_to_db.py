# scripts/insert_stopwords_to_db.py
import csv
import pymysql
import os
from dotenv import load_dotenv

# .env 파일에서 환경 변수를 로드합니다. (스크립트 실행 경로 기준)
load_dotenv(os.path.join(os.path.dirname(__file__), '..', '.env'))

# --- DB 설정 (config.py와 동일하게 설정) ---
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_PORT = int(os.getenv('DB_PORT', 3306))
DB_USER = os.getenv('DB_USER', 'root')
DB_PASSWORD = os.getenv('DB_PASSWORD', 'your_password') # ⭐ .env 파일에서 로드되도록 설정
DB_NAME = os.getenv('DB_NAME', 'your_database') # ⭐ .env 파일에서 로드되도록 설정

# --- CSV 파일 경로 (scripts/ 기준 상대 경로) ---
# 이 파일은 직접 생성하여 데이터를 채워야 합니다.
# 예시: python-nlp-service/data_source/stopwords.csv
DATA_SOURCE_DIR = os.path.join(os.path.dirname(__file__), '..', 'data_source')
STOPWORDS_CSV = os.path.join(DATA_SOURCE_DIR, 'stopwords.csv')

# ⭐ stopwords.csv 파일 예시:
# word
# 이
# 그
# 저
# 그리고
# ...


def insert_data_to_db(filepath: str, table_name: str, column_name: str):
    """CSV 파일을 읽어 DB 테이블에 데이터를 삽입합니다."""
    print(f"테이블 '{table_name}'에 '{filepath}' 파일의 데이터 삽입 시도 중...")
    if not os.path.exists(filepath):
        print(f"오류: 파일이 존재하지 않습니다: '{filepath}'. '{table_name}' 삽입을 건너뜁니다.")
        return

    conn = None
    try:
        conn = pymysql.connect(
            host=DB_HOST,
            user=DB_USER,
            password=DB_PASSWORD,
            db=DB_NAME,
            charset='utf8mb4'
        )
        cur = conn.cursor()

        # SQL 쿼리 준비: INSERT IGNORE는 중복 키가 있을 경우 오류 없이 무시합니다.
        insert_query = f"INSERT IGNORE INTO {table_name} ({column_name}) VALUES (%s)"

        with open(filepath, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f) # 헤더를 키로 사용하는 DictReader 사용

            for row in reader:
                try:
                    value = row[column_name] # 컬럼 이름으로 값 접근
                    if value: # 값이 비어있지 않은 경우에만 삽입
                        cur.execute(insert_query, (value,))
                except KeyError:
                    print(f"경고: CSV 파일에 컬럼 '{column_name}'이(가) 없습니다. 행을 건너뜁니다: {row}")
                except Exception as e:
                    print(f"오류: 행 '{row}'을(를) '{table_name}'에 삽입하지 못했습니다: {e}")

        conn.commit()
        print(f"'{filepath}' 파일의 데이터가 테이블 '{table_name}'에 성공적으로 삽입되었습니다.")

    except pymysql.Error as e:
        print(f"오류: '{table_name}'에 대한 DB 작업 실패: {e}")
    except Exception as e:
        print(f"오류: '{filepath}' 처리 중 예상치 못한 오류 발생: {e}")
    finally:
        if conn:
            cur.close()
            conn.close()

if __name__ == "__main__":
    print("--- 불용어 삽입 스크립트 실행 ---")

    # data_source 디렉토리가 없으면 생성 (CSV 파일들을 여기에 둡니다)
    os.makedirs(DATA_SOURCE_DIR, exist_ok=True)

    # 'stopwords' 테이블에 데이터 삽입
    # 'stopwords' 테이블에 'word' 컬럼이 있다고 가정합니다.
    insert_data_to_db(STOPWORDS_CSV, 'stopwords', 'word')

    print("--- 불용어 삽입 스크립트 완료 ---")
