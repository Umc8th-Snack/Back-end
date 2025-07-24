package umc.snack.service.quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.quiz.dto.QuizResponseDto;
import umc.snack.domain.quiz.entity.ArticleQuiz;
import umc.snack.domain.quiz.entity.Quiz;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.quiz.ArticleQuizRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class QuizService {
    
    // 기사 데이터를 데이터베이스에서 조회하거나 저장
    private final ArticleRepository articleRepository;
    // 기사와 퀴즈 연결정보
    private final ArticleQuizRepository articleQuizRepository;
    // JSON 문자열을 Map으로 파싱하는 객체
    private final ObjectMapper objectMapper;
    
    public QuizResponseDto getQuizzesByArticleId(Long articleId) {
        // 1. 기사 존재 여부 확인
        if (!articleRepository.existsById(articleId)) {
            throw new CustomException(ErrorCode.QUIZ_7601);
            // 기사가 존재하지 않는 경우 예외코드 7601
        }
        
        // 2. 기사에 연결된 퀴즈들 조회
        List<ArticleQuiz> articleQuizzes = articleQuizRepository.findByArticleIdWithQuiz(articleId);
        
        // 3. 퀴즈가 없는 경우 예외 처리
        if (articleQuizzes.isEmpty()) {
            throw new CustomException(ErrorCode.QUIZ_7601);
        }
        
        // 4. Quiz 엔티티들로부터 DTO 생성
        List<QuizResponseDto.QuizContentDto> quizContentList = articleQuizzes.stream()
                .map(articleQuiz -> parseQuizContent(articleQuiz.getQuiz()))
                .collect(Collectors.toList());
        
        return QuizResponseDto.builder()
                .quizContent(quizContentList)
                .build();
    }

    // Quiz 엔티티 하나를 받아서 클라이언트에게 보여줄 QuizContentDto 형태로 변환
    private QuizResponseDto.QuizContentDto parseQuizContent(Quiz quiz) {
        try {
            // JSON 문자열을 Map으로 파싱
            Map<String, Object> quizData = objectMapper.readValue(quiz.getQuizContent(), Map.class);
            
            String question = (String) quizData.get("question");
            @SuppressWarnings("unchecked")
            List<String> options = (List<String>) quizData.get("options");
            
            return QuizResponseDto.QuizContentDto.builder()
                    .quizId(quiz.getQuizId())
                    .question(question)
                    .options(options)
                    .build();
        } catch (JsonProcessingException e) {
            log.error("퀴즈 내용 파싱 오류: {}", e.getMessage());
            throw new CustomException(ErrorCode.SERVER_5101);
        }
    }
} 