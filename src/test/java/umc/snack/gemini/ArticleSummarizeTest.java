package umc.snack.gemini;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
            아래 뉴스 기사를 700글자 이내로 간결하게 요약해줘.
            기사 내용을 바탕으로 4지선다 객관식 퀴즈 2개를 만들어줘.
            각 퀴즈는 반드시 [문제, 보기 4개(choices), 정답(answer), 해설(explanation)]을 포함해줘.
            정답은 "{보기번호}. {보기내용}" 형식으로 되어야해.
            요약본에서 중고등학생이 이해하기 어려울 만한 단어 4개를 뽑아줘.
            단어의 뜻은 되도록 명사형 어미로 끝내줘.
            
            결과는 반드시 아래 JSON 형식으로만 응답해.
            
            {
              "summary": "...",
              "quizzes": [
                {
                  "question": "...",
                  "options": [ { "id": 1, "text": "..." }, { "id": 2, "text": "..." }],
                  "answer": "...",
                  "explanation": "..."
                }
              ],
              "terms": [
                { "word": "...", "meaning": "..." }
              ]
            }
            
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
        int total = articles.size();

        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            List<Article> batch = articles.subList(i, end);

            for (Article article : batch) {
                CrawledArticle crawled = crawledArticleRepository.findById(article.getArticleId())
                        .orElse(null);
                if (crawled == null) continue;

                String prompt = promptTemplate + crawled.getContent();
                String result = getCompletionWithRetry(prompt, "gemini-2.5-pro");

                log.info("기사 ID: {}", article.getArticleId());
                log.info("Gemini 결과: {}", result);
                log.info("=========================================================");

                geminiParsingService.updateArticleSummary(article.getArticleId(), result);

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