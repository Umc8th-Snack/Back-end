package umc.snack.service.article;

import io.jsonwebtoken.lang.Assert;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.article.entity.CrawledArticle;
import umc.snack.global.gemini.GeminiParsingService;
import umc.snack.global.gemini.GeminiService;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.article.CrawledArticleRepository;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;

@Service
public class ArticleSummarizeService {

    private static final Logger log = LoggerFactory.getLogger(ArticleSummarizeService.class);

    private final GeminiService geminiService;
    private final ArticleRepository articleRepository;
    private final CrawledArticleRepository crawledArticleRepository;
    private final GeminiParsingService geminiParsingService;

    public ArticleSummarizeService(
            GeminiService geminiService,
            ArticleRepository articleRepository,
            CrawledArticleRepository crawledArticleRepository,
            GeminiParsingService geminiParsingService
    ) {
        this.geminiService = geminiService;
        this.articleRepository = articleRepository;
        this.crawledArticleRepository = crawledArticleRepository;
        this.geminiParsingService = geminiParsingService;
    }
    String promptTemplate = """
            다음 지시를 정확히 따르세요.
                        
             [규칙]
             1. 요약: 한글 기준 공백 제외 700자 이내로 간결하게.
             2. 퀴즈: 요약된 기사 내용 기반 4지선다 객관식 2문항 작성.
                - 각 문항은 반드시 다음 키를 포함: question, options(1~4 id 포함), answer, explanation.
                - answer 형식: "{보기번호}. {보기내용}"
             3. 용어: 요약된 기사 기반 중·고등학생이 이해하기 어려울 만한 단어 4개 선정.
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

    // 자동 재시도 로직 (모델 overload 발생 시 최대 5회 재시도)
    private String getCompletionWithRetry(String prompt, String model) {
        int maxRetry = 5;
        long baseMillis = 2000; // 2s
        for (int i = 0; i < maxRetry; i++) {
            try {
                return geminiService.getCompletion(prompt, model);
            } catch (HttpServerErrorException ex) {
                int code = ex.getStatusCode().value();
                if (code == 503 || code == 429 || code == 502 || code == 504) {
                    long sleep = Math.min((long) Math.pow(2, i) * baseMillis, 120_000); // ≤120s
                    long jitter = ThreadLocalRandom.current().nextLong(0, 1000);
                    log.warn("{} 에러. {}번째 재시도 전 {}ms 대기", code, i + 1, sleep + jitter);
                    try { Thread.sleep(sleep + jitter); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw new RuntimeException(ie); }
                    continue;
                }
                throw ex;
            } catch (Exception e) {
                // 네트워크/타임아웃 등도 1회 재시도는 유효
                if (i < maxRetry - 1) {
                    try { Thread.sleep(1500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }
                throw e;
            }
        }
        throw new RuntimeException("Gemini 호출 재시도 실패");
    }

    public void getCompletion() {
        // 최근 7개만
        PageRequest page = PageRequest.of(0, 7, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Article> articlePage = articleRepository.findBySummaryIsNull(page);
        List<Article> articles = articlePage.getContent();

        if (articles.isEmpty()) {
            log.info("요약이 필요한 기사가 없습니다.");
            return;
        }

        for (Article article : articles) {
            CrawledArticle crawled = crawledArticleRepository
                    .findByArticleIdAndStatus(article.getArticleId(), CrawledArticle.Status.PROCESSED)
                    .orElse(null);

                log.info("CrawledArticle 조회 결과: {}", crawled); // 디버깅용 추가

                if (crawled == null) {
                    log.warn("PROCESSED 상태의 CrawledArticle이 없음 - {}", article.getArticleId());
                    continue;
                }

                String content = crawled.getContent();

                if (content == null || content.trim().isEmpty()) {
                    log.warn("기사 본문이 없음 - {}", article.getArticleId());
                    continue;
                }
            try {
                // Gemini API 호출
                String prompt = promptTemplate + crawled.getContent();
                String result = getCompletionWithRetry(prompt, "gemini-2.5-pro");

                log.info("Gemini 호출 결과 - articleId: {}, result: {}", article.getArticleId(), result);
                log.info("=========================================================");

                geminiParsingService.updateArticleSummary(article.getArticleId(), result);
            } catch (Exception e) {

                log.error("요약 실패 - articleId: {}", article.getArticleId(), e);
            }
            // 20초 대기
            try {
                Thread.sleep(20_000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }


}
