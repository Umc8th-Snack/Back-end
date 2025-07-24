-- ID가 1인 테스트용 사용자와 기사 데이터 삽입
-- ON DUPLICATE KEY UPDATE 구문은 데이터가 이미 있을 경우 에러 없이 덮어쓰게 해줍니다.
INSERT INTO user (id, name, email, created_at, updated_at) VALUES (1, 'testuser', 'test@example.com', NOW(), NOW()) ON DUPLICATE KEY UPDATE name=name;
INSERT INTO article (id, title, content, created_at, updated_at) VALUES (1, '테스트 기사 제목', '테스트 기사 내용입니다.', NOW(), NOW()) ON DUPLICATE KEY UPDATE title=title;