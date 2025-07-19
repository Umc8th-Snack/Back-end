package umc.snack.global.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.quiz.entity.ArticleQuiz;
import umc.snack.domain.quiz.entity.Quiz;
import umc.snack.domain.term.entity.ArticleTerm;
import umc.snack.domain.term.entity.Term;
import umc.snack.repository.article.ArticleQuizRepository;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.article.ArticleTermRepository;
import umc.snack.repository.quiz.QuizRepository;
import umc.snack.repository.term.TermRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class GeminiParsingService {

    private final ObjectMapper objectMapper;
    private final ArticleRepository articleRepository;
    private final QuizRepository quizRepository;
    private final TermRepository termRepository;
    private final ArticleQuizRepository articleQuizRepository;
    private final ArticleTermRepository articleTermRepository;

    // GeminiService에서 받은 JSON String을 Article의 summary에 반영
    @Transactional
    public void updateArticleSummary(Long articleId, String geminiJson) {
        // 마크다운 백틱 제거
        String cleanJson = geminiJson
                .replace("```json", "")
                .replace("```", "")
                .trim();
        try {
            // Gemini 결과 파싱
            GeminiResultDto geminiResult = objectMapper.readValue(cleanJson, GeminiResultDto.class);

            // Article 조회 및 summary 업데이트
            Article article = articleRepository.findById(articleId)
                    .orElseThrow(() -> new IllegalArgumentException("Article not found: " + articleId));
            article.updateSummary(geminiResult.getSummary());

            // 퀴즈 저장 (퀴즈-문제는 JSON 그대로 quizContent로 저장)
            List<GeminiResultDto.QuizDto> quizzes = geminiResult.getQuizzes();
            if (quizzes != null) {
                for (GeminiResultDto.QuizDto quizDto : quizzes) {
                    String quizContent = objectMapper.writeValueAsString(quizDto);
                    Quiz quiz = Quiz.builder().quizContent(quizContent).build();
                    quizRepository.save(quiz);

                    // ArticleQuiz 테이블로 연결
                    ArticleQuiz articleQuiz = ArticleQuiz.builder()
                            .articleId(article.getArticleId())
                            .quizId(quiz.getQuizId())
                            .build();
                    articleQuizRepository.save(articleQuiz);
                }
            }

            // 용어 저장 (terms)
            List<GeminiResultDto.TermDto> terms = geminiResult.getTerms();
            if (terms != null) {
                for (GeminiResultDto.TermDto termDto : terms) {
                    // term 존재 여부 확인
                    Term term = termRepository.findByWord(termDto.getWord())
                            .orElseGet(() -> termRepository.save(
                                    Term.builder()
                                            .word(termDto.getWord())
                                            .definition(termDto.getMeaning())
                                            .build()
                            ));
                    // article-term 관계 중복 확인 후 저장
                    boolean exists = articleTermRepository.findByArticleIdAndTermId(article.getArticleId(), term.getTermId())
                            .isPresent();
                    if (!exists) {
                        articleTermRepository.save(
                                ArticleTerm.builder()
                                        .articleId(article.getArticleId())
                                        .termId(term.getTermId())
                                        .build()
                        );
                    }
                }
            }

            // JPA 변경 감지로 자동 저장 (별도 save 불필요)
        } catch (Exception e) {
            // 로그 처리 또는 예외 처리
            throw new RuntimeException("Gemini JSON 파싱/업데이트 실패: " + e.getMessage(), e);
        }
    }
}
