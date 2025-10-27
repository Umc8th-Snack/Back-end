#!/usr/bin/env python3
import aiomysql
import asyncio
import sys
from datetime import datetime

async def insert_test_data():
    """3번 유저로 테스트 데이터를 추가합니다."""
    
    # 데이터베이스 연결 설정
    config = {
        'host': '127.0.0.1',
        'port': 3306,
        'user': 'root',
        'password': '1234abcd',
        'db': 'snack',
        'charset': 'utf8mb4'
    }
    
    try:
        # 데이터베이스 연결
        conn = await aiomysql.connect(**config)
        cursor = await conn.cursor()
        
        print("데이터베이스 연결 성공!")
        
        # 먼저 기사 ID들을 가져옵니다
        await cursor.execute("SELECT article_id FROM articles LIMIT 10")
        articles = await cursor.fetchall()
        
        if not articles:
            print("기사가 없습니다. 먼저 기사를 추가해야 합니다.")
            return
            
        print(f"총 {len(articles)}개의 기사를 찾았습니다.")
        
        # 3번 유저로 클릭 로그 추가
        user_id = 3
        current_time = datetime.now()
        
        for i, (article_id,) in enumerate(articles[:5]):  # 처음 5개 기사에 대해
            click_id = int(current_time.timestamp() * 1000) + i  # 고유한 click_id 생성
            
            # 클릭 로그 삽입
            await cursor.execute("""
                INSERT INTO user_clicks (click_id, user_id, article_id, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE updated_at = %s
            """, (click_id, user_id, article_id, current_time, current_time, current_time))
            
            print(f"클릭 로그 추가: 사용자 {user_id}, 기사 {article_id}")
        
        # 스크랩 로그도 추가
        for i, (article_id,) in enumerate(articles[:3]):  # 처음 3개 기사에 대해
            scrap_id = int(current_time.timestamp() * 1000) + 1000 + i  # 고유한 scrap_id 생성
            
            await cursor.execute("""
                INSERT INTO user_scraps (scrap_id, user_id, article_id, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE updated_at = %s
            """, (scrap_id, user_id, article_id, current_time, current_time, current_time))
            
            print(f"스크랩 로그 추가: 사용자 {user_id}, 기사 {article_id}")
        
        # 검색 로그도 추가
        search_keywords = ["정치", "경제", "사회", "스포츠", "문화"]
        for i, keyword in enumerate(search_keywords):
            search_id = int(current_time.timestamp() * 1000) + 2000 + i
            
            await cursor.execute("""
                INSERT INTO search_keywords (search_id, user_id, keyword, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE updated_at = %s
            """, (search_id, user_id, keyword, current_time, current_time, current_time))
            
            print(f"검색 로그 추가: 사용자 {user_id}, 키워드 '{keyword}'")
        
        # 변경사항 커밋
        await conn.commit()
        
        # 결과 확인
        await cursor.execute("SELECT COUNT(*) FROM user_clicks WHERE user_id = %s", (user_id,))
        click_count = await cursor.fetchone()
        
        await cursor.execute("SELECT COUNT(*) FROM user_scraps WHERE user_id = %s", (user_id,))
        scrap_count = await cursor.fetchone()
        
        await cursor.execute("SELECT COUNT(*) FROM search_keywords WHERE user_id = %s", (user_id,))
        search_count = await cursor.fetchone()
        
        print(f"\n=== 데이터 추가 완료 ===")
        print(f"사용자 {user_id}의 클릭 로그: {click_count[0]}개")
        print(f"사용자 {user_id}의 스크랩 로그: {scrap_count[0]}개")
        print(f"사용자 {user_id}의 검색 로그: {search_count[0]}개")
        
    except Exception as e:
        print(f"오류 발생: {e}")
        if 'conn' in locals():
            await conn.rollback()
    finally:
        if 'cursor' in locals():
            await cursor.close()
        if 'conn' in locals():
            conn.close()

if __name__ == "__main__":
    asyncio.run(insert_test_data())
