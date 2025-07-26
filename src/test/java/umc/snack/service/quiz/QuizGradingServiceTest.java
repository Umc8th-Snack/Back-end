package umc.snack.service.quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.quiz.dto.QuizGradingRequestDto;
import umc.snack.domain.quiz.dto.QuizGradingResponseDto;
import umc.snack.domain.quiz.entity.ArticleQuiz;
import umc.snack.domain.quiz.entity.Quiz;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.quiz.ArticleQuizRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("퀴즈 채점 서비스 테스트")
class QuizGradingServiceTest {

    @Mock
    private ArticleRepository articleRepository;
    
    @Mock
    private ArticleQuizRepository articleQuizRepository;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private QuizService quizService;
    
    private Long articleId;
    private Quiz quiz1;
    private Quiz quiz2;
    private ArticleQuiz articleQuiz1;
    private ArticleQuiz articleQuiz2;
    
    @BeforeEach
    void setUp() {
        articleId = 1L;
        
        // Quiz 엔티티 생성
        quiz1 = Quiz.builder()
                .quizId(1L)
                .quizContent("{\"question\":\"질문1\",\"options\":[\"답1\",\"답2\",\"답3\",\"답4\"],\"answer_index\":2,\"description\":\"정답 설명1\"}")
                .build();
        
        quiz2 = Quiz.builder()
                .quizId(2L)
                .quizContent("{\"question\":\"질문2\",\"options\":[\"답A\",\"답B\",\"답C\",\"답D\"],\"answer_index\":1,\"description\":\"정답 설명2\"}")
                .build();
        
        // ArticleQuiz 엔티티 생성
        articleQuiz1 = ArticleQuiz.builder()
                .articleId(articleId)
                .quizId(1L)
                .quiz(quiz1)
                .build();
        
        articleQuiz2 = ArticleQuiz.builder()
                .articleId(articleId)
                .quizId(2L)
                .quiz(quiz2)
                .build();
    }
    
    @Test
    @DisplayName("성공: 모든 답이 정답인 경우")
    void gradeQuizzes_AllCorrect() throws Exception {
        // given
        QuizGradingRequestDto requestDto = new QuizGradingRequestDto(Arrays.asList(
                new QuizGradingRequestDto.SubmittedAnswer(1L, 2), // 정답
                new QuizGradingRequestDto.SubmittedAnswer(2L, 1)  // 정답
        ));
        
        List<ArticleQuiz> articleQuizzes = Arrays.asList(articleQuiz1, articleQuiz2);
        
        when(articleRepository.existsById(articleId)).thenReturn(true);
        when(articleQuizRepository.findByArticleIdWithQuiz(articleId)).thenReturn(articleQuizzes);
        
        // JSON 파싱 Mock 설정
        when(objectMapper.readValue(quiz1.getQuizContent(), Map.class))
                .thenReturn(Map.of(
                        "question", "질문1",
                        "options", Arrays.asList("답1", "답2", "답3", "답4"),
                        "answer_index", 2,
                        "description", "정답 설명1"
                ));
        
        when(objectMapper.readValue(quiz2.getQuizContent(), Map.class))
                .thenReturn(Map.of(
                        "question", "질문2",
                        "options", Arrays.asList("답A", "답B", "답C", "답D"),
                        "answer_index", 1,
                        "description", "정답 설명2"
                ));
        
        // when
        QuizGradingResponseDto result = quizService.gradeQuizzes(articleId, requestDto);
        
        // then
        assertNotNull(result);
        assertNotNull(result.getDetails());
        assertEquals(2, result.getDetails().size());
        
        // 첫 번째 퀴즈 채점 결과 검증
        QuizGradingResponseDto.QuizGradingDetail detail1 = result.getDetails().get(0);
        assertEquals(1L, detail1.getQuizId());
        assertTrue(detail1.isCorrect());
        assertEquals(2, detail1.getSubmitted_answer());
        assertEquals(2, detail1.getAnswer_index());
        assertEquals("정답 설명1", detail1.getDescription());
        
        // 두 번째 퀴즈 채점 결과 검증
        QuizGradingResponseDto.QuizGradingDetail detail2 = result.getDetails().get(1);
        assertEquals(2L, detail2.getQuizId());
        assertTrue(detail2.isCorrect());
        assertEquals(1, detail2.getSubmitted_answer());
        assertEquals(1, detail2.getAnswer_index());
        assertEquals("정답 설명2", detail2.getDescription());
        
        // Mock 메소드 호출 검증
        verify(articleRepository).existsById(articleId);
        verify(articleQuizRepository).findByArticleIdWithQuiz(articleId);
        verify(objectMapper, times(2)).readValue(anyString(), eq(Map.class));
    }
    
    @Test
    @DisplayName("성공: 일부만 정답인 경우")
    void gradeQuizzes_PartiallyCorrect() throws Exception {
        // given
        QuizGradingRequestDto requestDto = new QuizGradingRequestDto(Arrays.asList(
                new QuizGradingRequestDto.SubmittedAnswer(1L, 0), // 오답 (정답: 2)
                new QuizGradingRequestDto.SubmittedAnswer(2L, 1)  // 정답
        ));
        
        List<ArticleQuiz> articleQuizzes = Arrays.asList(articleQuiz1, articleQuiz2);
        
        when(articleRepository.existsById(articleId)).thenReturn(true);
        when(articleQuizRepository.findByArticleIdWithQuiz(articleId)).thenReturn(articleQuizzes);
        
        when(objectMapper.readValue(quiz1.getQuizContent(), Map.class))
                .thenReturn(Map.of("answer_index", 2, "description", "정답 설명1"));
        
        when(objectMapper.readValue(quiz2.getQuizContent(), Map.class))
                .thenReturn(Map.of("answer_index", 1, "description", "정답 설명2"));
        
        // when
        QuizGradingResponseDto result = quizService.gradeQuizzes(articleId, requestDto);
        
        // then
        assertEquals(2, result.getDetails().size());
        
        // 첫 번째는 오답
        QuizGradingResponseDto.QuizGradingDetail detail1 = result.getDetails().get(0);
        assertFalse(detail1.isCorrect());
        assertEquals(0, detail1.getSubmitted_answer());
        assertEquals(2, detail1.getAnswer_index());
        
        // 두 번째는 정답
        QuizGradingResponseDto.QuizGradingDetail detail2 = result.getDetails().get(1);
        assertTrue(detail2.isCorrect());
        assertEquals(1, detail2.getSubmitted_answer());
        assertEquals(1, detail2.getAnswer_index());
    }
    
    @Test
    @DisplayName("실패: 기사가 존재하지 않는 경우")
    void gradeQuizzes_ArticleNotFound() {
        // given
        QuizGradingRequestDto requestDto = new QuizGradingRequestDto(Arrays.asList(
                new QuizGradingRequestDto.SubmittedAnswer(1L, 2)
        ));
        
        when(articleRepository.existsById(articleId)).thenReturn(false);
        
        // when & then
        CustomException exception = assertThrows(CustomException.class, 
                () -> quizService.gradeQuizzes(articleId, requestDto));
        
        assertEquals(ErrorCode.QUIZ_7601, exception.getErrorCode());
        
        verify(articleRepository).existsById(articleId);
        verifyNoInteractions(articleQuizRepository);
        verifyNoInteractions(objectMapper);
    }
    
    @Test
    @DisplayName("실패: 기사에 퀴즈가 없는 경우")
    void gradeQuizzes_NoQuizzes() {
        // given
        QuizGradingRequestDto requestDto = new QuizGradingRequestDto(Arrays.asList(
                new QuizGradingRequestDto.SubmittedAnswer(1L, 2)
        ));
        
        when(articleRepository.existsById(articleId)).thenReturn(true);
        when(articleQuizRepository.findByArticleIdWithQuiz(articleId)).thenReturn(Collections.emptyList());
        
        // when & then
        CustomException exception = assertThrows(CustomException.class, 
                () -> quizService.gradeQuizzes(articleId, requestDto));
        
        assertEquals(ErrorCode.QUIZ_7602, exception.getErrorCode());
        
        verify(articleRepository).existsById(articleId);
        verify(articleQuizRepository).findByArticleIdWithQuiz(articleId);
        verifyNoInteractions(objectMapper);
    }
    
    @Test
    @DisplayName("실패: 유효하지 않은 퀴즈 ID")
    void gradeQuizzes_InvalidQuizId() {
        // given
        QuizGradingRequestDto requestDto = new QuizGradingRequestDto(Arrays.asList(
                new QuizGradingRequestDto.SubmittedAnswer(999L, 2) // 존재하지 않는 퀴즈 ID
        ));
        
        List<ArticleQuiz> articleQuizzes = Arrays.asList(articleQuiz1, articleQuiz2); // 1L, 2L만 존재
        
        when(articleRepository.existsById(articleId)).thenReturn(true);
        when(articleQuizRepository.findByArticleIdWithQuiz(articleId)).thenReturn(articleQuizzes);
        
        // when & then
        CustomException exception = assertThrows(CustomException.class, 
                () -> quizService.gradeQuizzes(articleId, requestDto));
        
        assertEquals(ErrorCode.QUIZ_7603, exception.getErrorCode());
        
        verify(articleRepository).existsById(articleId);
        verify(articleQuizRepository).findByArticleIdWithQuiz(articleId);
        verifyNoInteractions(objectMapper);
    }
    
    @Test
    @DisplayName("실패: JSON 파싱 오류")
    void gradeQuizzes_JsonParsingError() throws Exception {
        // given
        QuizGradingRequestDto requestDto = new QuizGradingRequestDto(Arrays.asList(
                new QuizGradingRequestDto.SubmittedAnswer(1L, 2)
        ));
        
        List<ArticleQuiz> articleQuizzes = Arrays.asList(articleQuiz1);
        
        when(articleRepository.existsById(articleId)).thenReturn(true);
        when(articleQuizRepository.findByArticleIdWithQuiz(articleId)).thenReturn(articleQuizzes);
        when(objectMapper.readValue(quiz1.getQuizContent(), Map.class))
                .thenThrow(new JsonProcessingException("JSON 파싱 오류") {});
        
        // when & then
        CustomException exception = assertThrows(CustomException.class, 
                () -> quizService.gradeQuizzes(articleId, requestDto));
        
        assertEquals(ErrorCode.SERVER_5101, exception.getErrorCode());
        
        verify(articleRepository).existsById(articleId);
        verify(articleQuizRepository).findByArticleIdWithQuiz(articleId);
        verify(objectMapper).readValue(quiz1.getQuizContent(), Map.class);
    }
    
    @Test
    @DisplayName("성공: 4개 퀴즈 모두 채점")
    void gradeQuizzes_FourQuizzes() throws Exception {
        // given
        Quiz quiz3 = Quiz.builder().quizId(3L).quizContent("quiz3_content").build();
        Quiz quiz4 = Quiz.builder().quizId(4L).quizContent("quiz4_content").build();
        
        ArticleQuiz articleQuiz3 = ArticleQuiz.builder()
                .articleId(articleId).quizId(3L).quiz(quiz3).build();
        ArticleQuiz articleQuiz4 = ArticleQuiz.builder()
                .articleId(articleId).quizId(4L).quiz(quiz4).build();
        
        QuizGradingRequestDto requestDto = new QuizGradingRequestDto(Arrays.asList(
                new QuizGradingRequestDto.SubmittedAnswer(1L, 2),
                new QuizGradingRequestDto.SubmittedAnswer(2L, 1),
                new QuizGradingRequestDto.SubmittedAnswer(3L, 0),
                new QuizGradingRequestDto.SubmittedAnswer(4L, 3)
        ));
        
        List<ArticleQuiz> articleQuizzes = Arrays.asList(articleQuiz1, articleQuiz2, articleQuiz3, articleQuiz4);
        
        when(articleRepository.existsById(articleId)).thenReturn(true);
        when(articleQuizRepository.findByArticleIdWithQuiz(articleId)).thenReturn(articleQuizzes);
        
        // 각 퀴즈에 대한 JSON 파싱 Mock 설정
        when(objectMapper.readValue(quiz1.getQuizContent(), Map.class))
                .thenReturn(Map.of("answer_index", 2, "description", "설명1"));
        when(objectMapper.readValue(quiz2.getQuizContent(), Map.class))
                .thenReturn(Map.of("answer_index", 1, "description", "설명2"));
        when(objectMapper.readValue(quiz3.getQuizContent(), Map.class))
                .thenReturn(Map.of("answer_index", 0, "description", "설명3"));
        when(objectMapper.readValue(quiz4.getQuizContent(), Map.class))
                .thenReturn(Map.of("answer_index", 3, "description", "설명4"));
        
        // when
        QuizGradingResponseDto result = quizService.gradeQuizzes(articleId, requestDto);
        
        // then
        assertEquals(4, result.getDetails().size());
        
        // 모든 퀴즈가 정답인지 확인
        long correctCount = result.getDetails().stream()
                .mapToLong(detail -> detail.isCorrect() ? 1 : 0)
                .sum();
        assertEquals(4, correctCount);
        
        // Mock 메소드 호출 검증
        verify(objectMapper, times(4)).readValue(anyString(), eq(Map.class));
    }
} 