#!/usr/bin/env python3
import aiomysql
import asyncio

async def check_user_data():
    """3번 유저의 데이터를 확인합니다."""
    
    config = {
        'host': '127.0.0.1',
        'port': 3306,
        'user': 'root',
        'password': '1234abcd',
        'db': 'snack',
        'charset': 'utf8mb4'
    }
    
    try:
        conn = await aiomysql.connect(**config)
        cursor = await conn.cursor()
        
        user_id = 3
        
        # 클릭 로그 확인
        await cursor.execute("""
            SELECT uc.click_id, uc.article_id, uc.created_at, a.title 
            FROM user_clicks uc 
            JOIN articles a ON uc.article_id = a.article_id 
            WHERE uc.user_id = %s 
            ORDER BY uc.created_at DESC
        """, (user_id,))
        clicks = await cursor.fetchall()
        
        print(f"=== 사용자 {user_id}의 클릭 로그 ===")
        for click in clicks:
            print(f"클릭 ID: {click[0]}, 기사 ID: {click[1]}, 제목: {click[3][:50]}..., 시간: {click[2]}")
        
        # 스크랩 로그 확인
        await cursor.execute("""
            SELECT us.scrap_id, us.article_id, us.created_at, a.title 
            FROM user_scraps us 
            JOIN articles a ON us.article_id = a.article_id 
            WHERE us.user_id = %s 
            ORDER BY us.created_at DESC
        """, (user_id,))
        scraps = await cursor.fetchall()
        
        print(f"\n=== 사용자 {user_id}의 스크랩 로그 ===")
        for scrap in scraps:
            print(f"스크랩 ID: {scrap[0]}, 기사 ID: {scrap[1]}, 제목: {scrap[3][:50]}..., 시간: {scrap[2]}")
        
        # 검색 로그 확인
        await cursor.execute("""
            SELECT search_id, keyword, created_at 
            FROM search_keywords 
            WHERE user_id = %s 
            ORDER BY created_at DESC
        """, (user_id,))
        searches = await cursor.fetchall()
        
        print(f"\n=== 사용자 {user_id}의 검색 로그 ===")
        for search in searches:
            print(f"검색 ID: {search[0]}, 키워드: '{search[1]}', 시간: {search[2]}")
        
        print(f"\n=== 요약 ===")
        print(f"클릭 로그: {len(clicks)}개")
        print(f"스크랩 로그: {len(scraps)}개")
        print(f"검색 로그: {len(searches)}개")
        
    except Exception as e:
        print(f"오류 발생: {e}")
    finally:
        if 'cursor' in locals():
            await cursor.close()
        if 'conn' in locals():
            conn.close()

if __name__ == "__main__":
    asyncio.run(check_user_data())
