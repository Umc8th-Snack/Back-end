package umc.snack.gemini;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.HttpServerErrorException;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.article.entity.CrawledArticle;
import umc.snack.global.gemini.GeminiParsingService;
import umc.snack.global.gemini.GeminiService;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.article.CrawledArticleRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class ArticleSummarizeTest {

    private static final Logger log = LoggerFactory.getLogger(ArticleSummarizeTest.class);

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private CrawledArticleRepository crawledArticleRepository;

    @Autowired
    private GeminiParsingService geminiParsingService;

    String promptTemplate = """
            다음 지시를 정확히 따르세요.
                        
             [규칙]
             1. 요약: 한글 기준 공백 제외 700자 이내로 간결하게.
             2. 퀴즈: 기사 내용 기반 4지선다 객관식 2문항 작성.
                - 각 문항은 반드시 다음 키를 포함: question, options(1~4 id 포함), answer, explanation.
                - answer 형식: "{보기번호}. {보기내용}"
             3. 용어: 중·고등학생이 이해하기 어려울 만한 단어 4개 선정.
                - meaning은 품사에 맞게: 명사는 ‘…명사형 어미’, 형용사는 ‘…형용사형 어미’로 끝낼 것.
                - meaning의 끝에 '.' 붙이지 말 것.
                - 각 항목에 word, meaning.
                - word는 한자를 포함하지 말 것.
             4. 출력은 아래 JSON 스키마와 동일해야 하며, 추가 키/텍스트/주석/마크다운 금지.
                - "answer"는 반드시 {"id": 번호, "text": 보기내용}의 object로 작성할 것.
                - "answer": "4. 보기내용"과 같이 문자열로 출력하지 말 것.
            
             [출력 JSON 스키마]
             {
               "summary": "…",
               "quizzes": [
                 {
                   "question": "…",
                   "options": [
                     { "id": 1, "text": "…" },
                     { "id": 2, "text": "…" },
                     { "id": 3, "text": "…" },
                     { "id": 4, "text": "…" }
                   ],
                   "answer": { "id": "…", "text": "…" },
                   "explanation": "…"
                 },
                 {
                   "question": "…",
                   "options": [
                     { "id": 1, "text": "…" },
                     { "id": 2, "text": "…" },
                     { "id": 3, "text": "…" },
                     { "id": 4, "text": "…" }
                   ],
                   "answer": { "id": "…", "text": "…" },
                   "explanation": "…"
                 }
               ],
               "terms": [
                 { "word": "…", "meaning": "…" },
                 { "word": "…", "meaning": "…" },
                 { "word": "…", "meaning": "…" },
                 { "word": "…", "meaning": "…" }
               ]
             }
            
             [검증]
             - JSON 파싱 가능해야 함.
             - 글자수 초과 시 요약을 더 줄일 것.
             - 모든 따옴표는 쌍따옴표 사용.
             - 누락된 키 없어야 함.
            
            
             [입력]
             기사 본문:
             
            """;

    // 자동 재시도 로직 (모델 overload 발생 시 최대 10회 재시도)
    private String getCompletionWithRetry(String prompt, String model) {
        int maxRetry = 10;
        for (int i = 0; i < maxRetry; i++) {
            try {
                return geminiService.getCompletion(prompt, model);
            } catch (HttpServerErrorException ex) {
                if (ex.getStatusCode().value() == 503) {
                    log.warn("503 에러 발생. {}번째 재시도 대기 중...", i + 1);
                    try {
                        Thread.sleep((long) Math.pow(2, i) * 1000); // 지수 백오프
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("재시도 중 인터럽트 발생", e);
                    }
                    continue;
                }
                throw ex;
            }
        }
        throw new RuntimeException("Gemini model overload로 재시도 실패");
    }

    @Test
    void getCompletion() {
        List<Article> articles = articleRepository.findBySummaryIsNull();

        if (articles.isEmpty()) {
            log.info("요약이 필요한 기사가 없습니다.");
            return;
        }

        int batchSize = 5;
        //int total = articles.size();
        int total = Math.min(articles.size(), 5); // 최신 기사 5개까지만 실행(테스트용)

        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            List<Article> batch = articles.subList(i, end);

            for (Article article : batch) {

                log.info("기사 처리 시작 - ID: {}", article.getArticleId()); // 디버깅용 추가

//                CrawledArticle crawled = crawledArticleRepository.findById(article.getArticleId())
//                        .orElse(null);
                CrawledArticle crawled = crawledArticleRepository.findByArticleId(article.getArticleId()).orElse(null);

                log.info("CrawledArticle 조회 결과: {}", crawled); // 디버깅용 추가

                if (crawled == null) {
                    log.warn("CrawledArticle이 없음 - {}", article.getArticleId());
                    continue;
                }

                String content = crawled.getContent(); // 디버깅용 추가

                if (content == null || content.trim().isEmpty()) {
                    log.warn("기사 본문이 없음 - {}", article.getArticleId());
                    continue;
                } // 디버깅용 추가

                String prompt = promptTemplate + crawled.getContent();
                String result = getCompletionWithRetry(prompt, "gemini-2.5-pro");

                log.info("기사 ID: {}", article.getArticleId());
                log.info("Gemini 결과: {}", result);
                log.info("=========================================================");

                /*articles에 전에 저장했던 기사들이 남아 있어서 crawled_articles와 articles에서 article_id가 같은 것을 확인하고
                 해당 article_id를 가진 summary 에 요약을 저장하게 할 수 있을까요?*/
                geminiParsingService.updateArticleSummary(crawled.getArticleId(), result);

                // 1개 처리 후 10초 대기 (overload 방지)
                try {
                    Thread.sleep(10_000); // 고정 10초 대기
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                Article updatedArticle = articleRepository.findById(article.getArticleId()).orElse(null);
                assertNotNull(updatedArticle.getSummary(), "요약이 생성되어야 합니다.");
            }

            // 5개마다 추가 대기
            try {
                log.info("== 5개 처리 후 잠시 대기 ==");
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}