package umc.snack.service.quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.quiz.dto.QuizGradingRequestDto;
import umc.snack.domain.quiz.dto.QuizGradingResponseDto;
import umc.snack.domain.quiz.dto.QuizResponseDto;
import umc.snack.domain.quiz.entity.ArticleQuiz;
import umc.snack.domain.quiz.entity.Quiz;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.quiz.ArticleQuizRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
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

            // options가 객체 배열 형태이므로 text 필드만 추출
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> optionObjects = (List<Map<String, Object>>) quizData.get("options");

            List<String> options = optionObjects.stream()
                    .map(option -> (String) option.get("text"))
                    .collect(Collectors.toList());
            
            return QuizResponseDto.QuizContentDto.builder()
                    .quizId(quiz.getQuizId())
                    .question(question)
                    .options(options)
                    .build();
        } catch (JsonProcessingException e) {
            log.error("퀴즈 내용 파싱 오류: {}", e.getMessage());
            throw new CustomException(ErrorCode.SERVER_5101);
        } catch (ClassCastException e) {
            log.error("퀴즈 JSON 구조 파싱 오류: {}", e.getMessage());
            throw new CustomException(ErrorCode.SERVER_5101);
        }
    }
    
    @Transactional
    public QuizGradingResponseDto gradeQuizzes(Long articleId, QuizGradingRequestDto requestDto) {
        // 1. 기사 존재 여부 확인
        if (!articleRepository.existsById(articleId)) {
            throw new CustomException(ErrorCode.QUIZ_7601);
        }
        
        // 2. 기사에 연결된 퀴즈들 조회
        List<ArticleQuiz> articleQuizzes = articleQuizRepository.findByArticleIdWithQuiz(articleId);
        
        if (articleQuizzes.isEmpty()) {
            throw new CustomException(ErrorCode.QUIZ_7602);
        }
        
        // 3. 기사에 속한 퀴즈 ID들을 Set으로 만들어서 유효성 검증 준비
        Set<Long> validQuizIds = articleQuizzes.stream()
                .map(ArticleQuiz::getQuizId)
                .collect(Collectors.toSet());
        
        // 4. 제출된 퀴즈 ID들이 모두 해당 기사에 속하는지 검증

        List<Long> submittedQuizIds = requestDto.getSubmittedAnswers().stream()
                .map(QuizGradingRequestDto.SubmittedAnswer::getQuizId)
                .collect(Collectors.toList());

        // 없으면 에러 반환
        for (Long submittedQuizId : submittedQuizIds) {
            if (!validQuizIds.contains(submittedQuizId)) {
                throw new CustomException(ErrorCode.QUIZ_7603);
            }
        }

        // 채점 준비 - 퀴즈 ID를 키, 퀴즈 객체를 값으로 하는 Map
        // 5. 퀴즈 ID를 키로 하는 Map 생성 (빠른 조회를 위해)
        Map<Long, Quiz> quizMap = articleQuizzes.stream()
                .collect(Collectors.toMap(ArticleQuiz::getQuizId, ArticleQuiz::getQuiz));
        
        // 6. 각 퀴즈별 채점 수행
        // 사용자가 제출한 답안 목록을 하나씩 꺼내 gradeIndividualQuiz 메소드로 보내 채점
        List<QuizGradingResponseDto.QuizGradingDetail> gradingDetails = requestDto.getSubmittedAnswers().stream()
                .map(submittedAnswer -> gradeIndividualQuiz(submittedAnswer, quizMap.get(submittedAnswer.getQuizId())))
                .collect(Collectors.toList());

        // 채점결과 QuizGradingResponseDto에 담아 반환
        return QuizGradingResponseDto.builder()
                .details(gradingDetails)
                .build();
    }

    // 퀴즈 하나 채점 메소드
    private QuizGradingResponseDto.QuizGradingDetail gradeIndividualQuiz(
            QuizGradingRequestDto.SubmittedAnswer submittedAnswer, Quiz quiz) {
        try {
            // JSON 문자열을 Map으로 파싱
            Map<String, Object> quizData = objectMapper.readValue(quiz.getQuizContent(), Map.class);
            
            // 정답 인덱스와 설명 추출
            int correctAnswerIndex = (Integer) quizData.get("answer_index");
            String description = (String) quizData.get("description");
            
            // 채점 결과 계산
            boolean isCorrect = submittedAnswer.getSubmitted_answer_index() == correctAnswerIndex;

            // 채점 결과 QuizGradingDetail 객체로 반환함
            return QuizGradingResponseDto.QuizGradingDetail.builder()
                    .quizId(submittedAnswer.getQuizId())
                    .isCorrect(isCorrect)
                    .submitted_answer(submittedAnswer.getSubmitted_answer_index())
                    .answer_index(correctAnswerIndex)
                    .description(description)
                    .build();
                    
        } catch (JsonProcessingException e) {
            log.error("퀴즈 채점 중 JSON 파싱 오류: {}", e.getMessage());
            throw new CustomException(ErrorCode.SERVER_5101);
        }
    }
} 