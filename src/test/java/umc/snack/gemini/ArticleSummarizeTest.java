package umc.snack.gemini;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import umc.snack.domain.article.entity.CrawledArticle;
import umc.snack.global.gemini.GeminiService;
import umc.snack.repository.article.CrawledArticleRepository;

import java.util.List;

@SpringBootTest
public class ArticleSummarizeTest {

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private CrawledArticleRepository crawledArticleRepository;

    String promptTemplate = """
            아래 뉴스 기사를 700글자 이내로 간결하게 요약해줘.
            기사 내용을 바탕으로 4지선다 객관식 퀴즈 2개를 만들어줘.
            각 퀴즈는 반드시 [문제, 보기 4개(choices), 정답(answer), 해설(explanation)]을 포함해줘.
            정답은 "{보기번호}. {보기내용}" 형식으로 되어야해.
            요약본에서 중고등학생이 이해하기 어려울 만한 단어 4개를 뽑아줘.
            
            결과는 반드시 아래 JSON 형식으로만 응답해.
            
            {
              "summary": "...",
              "quizzes": [
                {
                  "question": "...",
                  "choices": ["...", "...", "...", "..."],
                  "answer": "...",
                  "explanation": "..."
                }
              ],
              "difficult_words": [
                { "word": "...", "meaning": "..." }
              ]
            }
            
            기사 본문:
            """;

    @Test
    void getCompletion() {
        // PROCESSED 상태 기사만 가져오기
        List<CrawledArticle> articles =
                crawledArticleRepository.findByStatus(CrawledArticle.Status.PROCESSED);

        // 각 기사 content로 Gemini 호출
        // 한 batch = 5 개씩 처리
        int batchSize = 5;
        int total = articles.size();

        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            List<CrawledArticle> batch = articles.subList(i, end);

            for (CrawledArticle article : batch) {
                String prompt = promptTemplate + article.getContent();
                String result = geminiService.getCompletion(prompt, "gemini-2.5-pro");

                System.out.println("기사 ID: " + article.getCrawledArticleId());
                System.out.println("Gemini 결과: " + result);
                System.out.println("=========================================================");
            }

            // 5개 호출 시마다 10초 대기 -> model overload 방지
            try {
                System.out.println("== 5개 처리 후 잠시 대기 ==");
                Thread.sleep(10_000); // 10초
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
