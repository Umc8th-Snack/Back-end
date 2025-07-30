package umc.snack.service.quiz;

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
import umc.snack.domain.quiz.dto.QuizResponseDto;
import umc.snack.domain.quiz.entity.ArticleQuiz;
import umc.snack.domain.quiz.entity.Quiz;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.quiz.ArticleQuizRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuizService 테스트")
class QuizServiceTest {

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
                .quizContent("{\"question\":\"이 기사의 핵심 주제는 무엇인가요?\",\"options\":[\"경제\",\"사회\",\"기술\",\"정치\"]}")
                .build();
        
        quiz2 = Quiz.builder()
                .quizId(2L)
                .quizContent("{\"question\":\"기사에서 언급된 주요 기술의 이름은 무엇인가요?\",\"options\":[\"블록체인\",\"인공지능\",\"양자컴퓨팅\",\"사물인터넷\"]}")
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
    @DisplayName("성공: 기사가 존재하고 퀴즈도 존재하는 경우")
    void getQuizzesByArticleId_Success() throws Exception {
        // given
        List<ArticleQuiz> articleQuizzes = Arrays.asList(articleQuiz1, articleQuiz2);
        
        when(articleRepository.existsById(articleId)).thenReturn(true);
        when(articleQuizRepository.findByArticleIdWithQuiz(articleId)).thenReturn(articleQuizzes);
        
        // JSON 파싱 Mock 설정 - 실제 JSON 구조에 맞게 수정
        when(objectMapper.readValue(quiz1.getQuizContent(), java.util.Map.class))
                .thenReturn(java.util.Map.of(
                        "question", "이 기사의 핵심 주제는 무엇인가요?",
                        "options", Arrays.asList(
                                java.util.Map.of("id", 1, "text", "경제"),
                                java.util.Map.of("id", 2, "text", "사회"),
                                java.util.Map.of("id", 3, "text", "기술"),
                                java.util.Map.of("id", 4, "text", "정치")
                        )
                ));
        
        when(objectMapper.readValue(quiz2.getQuizContent(), java.util.Map.class))
                .thenReturn(java.util.Map.of(
                        "question", "기사에서 언급된 주요 기술의 이름은 무엇인가요?",
                        "options", Arrays.asList(
                                java.util.Map.of("id", 1, "text", "블록체인"),
                                java.util.Map.of("id", 2, "text", "인공지능"),
                                java.util.Map.of("id", 3, "text", "양자컴퓨팅"),
                                java.util.Map.of("id", 4, "text", "사물인터넷")
                        )
                ));
        
        // when
        QuizResponseDto result = quizService.getQuizzesByArticleId(articleId);
        
        // then
        assertNotNull(result);
        assertNotNull(result.getQuizContent());
        assertEquals(2, result.getQuizContent().size());
        
        // 첫 번째 퀴즈 검증
        QuizResponseDto.QuizContentDto quiz1Dto = result.getQuizContent().get(0);
        assertEquals(1L, quiz1Dto.getQuizId());
        assertEquals("이 기사의 핵심 주제는 무엇인가요?", quiz1Dto.getQuestion());
        assertEquals(Arrays.asList("경제", "사회", "기술", "정치"), quiz1Dto.getOptions());
        
        // 두 번째 퀴즈 검증
        QuizResponseDto.QuizContentDto quiz2Dto = result.getQuizContent().get(1);
        assertEquals(2L, quiz2Dto.getQuizId());
        assertEquals("기사에서 언급된 주요 기술의 이름은 무엇인가요?", quiz2Dto.getQuestion());
        assertEquals(Arrays.asList("블록체인", "인공지능", "양자컴퓨팅", "사물인터넷"), quiz2Dto.getOptions());
        
        // Mock 메소드 호출 검증
        verify(articleRepository).existsById(articleId);
        verify(articleQuizRepository).findByArticleIdWithQuiz(articleId);
        verify(objectMapper, times(2)).readValue(anyString(), eq(java.util.Map.class));
    }
    
    @Test
    @DisplayName("실패: 기사가 존재하지 않는 경우 - QUIZ_7601")
    void getQuizzesByArticleId_ArticleNotFound() {
        // given
        when(articleRepository.existsById(articleId)).thenReturn(false);
        
        // when & then
        CustomException exception = assertThrows(CustomException.class, 
                () -> quizService.getQuizzesByArticleId(articleId));
        
        assertEquals(ErrorCode.QUIZ_7601, exception.getErrorCode());
        
        // Mock 메소드 호출 검증
        verify(articleRepository).existsById(articleId);
        verifyNoInteractions(articleQuizRepository);
        verifyNoInteractions(objectMapper);
    }
    
    @Test
    @DisplayName("실패: 기사는 존재하지만 퀴즈가 없는 경우 - QUIZ_7601")
    void getQuizzesByArticleId_NoQuizzesFound() {
        // given
        when(articleRepository.existsById(articleId)).thenReturn(true);
        when(articleQuizRepository.findByArticleIdWithQuiz(articleId)).thenReturn(Collections.emptyList());
        
        // when & then
        CustomException exception = assertThrows(CustomException.class, 
                () -> quizService.getQuizzesByArticleId(articleId));
        
        assertEquals(ErrorCode.QUIZ_7601, exception.getErrorCode());
        
        // Mock 메소드 호출 검증
        verify(articleRepository).existsById(articleId);
        verify(articleQuizRepository).findByArticleIdWithQuiz(articleId);
        verifyNoInteractions(objectMapper);
    }
    
    @Test
    @DisplayName("실패: JSON 파싱 오류 발생 - SERVER_5101")
    void getQuizzesByArticleId_JsonParsingError() throws Exception {
        // given
        List<ArticleQuiz> articleQuizzes = Arrays.asList(articleQuiz1);
        
        when(articleRepository.existsById(articleId)).thenReturn(true);
        when(articleQuizRepository.findByArticleIdWithQuiz(articleId)).thenReturn(articleQuizzes);
        when(objectMapper.readValue(quiz1.getQuizContent(), java.util.Map.class))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("JSON 파싱 오류") {});
        
        // when & then
        CustomException exception = assertThrows(CustomException.class, 
                () -> quizService.getQuizzesByArticleId(articleId));
        
        assertEquals(ErrorCode.SERVER_5101, exception.getErrorCode());
        
        // Mock 메소드 호출 검증
        verify(articleRepository).existsById(articleId);
        verify(articleQuizRepository).findByArticleIdWithQuiz(articleId);
        verify(objectMapper).readValue(quiz1.getQuizContent(), java.util.Map.class);
    }
    
    @Test
    @DisplayName("성공: 단일 퀴즈만 존재하는 경우")
    void getQuizzesByArticleId_SingleQuiz() throws Exception {
        // given
        List<ArticleQuiz> articleQuizzes = Arrays.asList(articleQuiz1);
        
        when(articleRepository.existsById(articleId)).thenReturn(true);
        when(articleQuizRepository.findByArticleIdWithQuiz(articleId)).thenReturn(articleQuizzes);
        when(objectMapper.readValue(quiz1.getQuizContent(), java.util.Map.class))
                .thenReturn(java.util.Map.of(
                        "question", "이 기사의 핵심 주제는 무엇인가요?",
                        "options", Arrays.asList(
                                java.util.Map.of("id", 1, "text", "경제"),
                                java.util.Map.of("id", 2, "text", "사회"),
                                java.util.Map.of("id", 3, "text", "기술"),
                                java.util.Map.of("id", 4, "text", "정치")
                        )
                ));
        
        // when
        QuizResponseDto result = quizService.getQuizzesByArticleId(articleId);
        
        // then
        assertNotNull(result);
        assertEquals(1, result.getQuizContent().size());
        
        QuizResponseDto.QuizContentDto quizDto = result.getQuizContent().get(0);
        assertEquals(1L, quizDto.getQuizId());
        assertEquals("이 기사의 핵심 주제는 무엇인가요?", quizDto.getQuestion());
        assertEquals(4, quizDto.getOptions().size());
    }
    
    @Test
    @DisplayName("성공: 4개의 퀴즈가 모두 존재하는 경우")
    void getQuizzesByArticleId_FourQuizzes() throws Exception {
        // given
        Quiz quiz3 = Quiz.builder()
                .quizId(3L)
                .quizContent("{\"question\":\"세 번째 질문\",\"options\":[\"옵션1\",\"옵션2\",\"옵션3\",\"옵션4\"]}")
                .build();
        
        Quiz quiz4 = Quiz.builder()
                .quizId(4L)
                .quizContent("{\"question\":\"네 번째 질문\",\"options\":[\"답1\",\"답2\",\"답3\",\"답4\"]}")
                .build();
        
        ArticleQuiz articleQuiz3 = ArticleQuiz.builder()
                .articleId(articleId)
                .quizId(3L)
                .quiz(quiz3)
                .build();
        
        ArticleQuiz articleQuiz4 = ArticleQuiz.builder()
                .articleId(articleId)
                .quizId(4L)
                .quiz(quiz4)
                .build();
        
        List<ArticleQuiz> articleQuizzes = Arrays.asList(articleQuiz1, articleQuiz2, articleQuiz3, articleQuiz4);
        
        when(articleRepository.existsById(articleId)).thenReturn(true);
        when(articleQuizRepository.findByArticleIdWithQuiz(articleId)).thenReturn(articleQuizzes);
        
        // 각 퀴즈에 대한 JSON 파싱 Mock 설정 - 실제 JSON 구조에 맞게 수정
        when(objectMapper.readValue(quiz1.getQuizContent(), java.util.Map.class))
                .thenReturn(java.util.Map.of("question", "질문1", "options", Arrays.asList(
                        java.util.Map.of("id", 1, "text", "옵션1"),
                        java.util.Map.of("id", 2, "text", "옵션2"),
                        java.util.Map.of("id", 3, "text", "옵션3"),
                        java.util.Map.of("id", 4, "text", "옵션4")
                )));
        when(objectMapper.readValue(quiz2.getQuizContent(), java.util.Map.class))
                .thenReturn(java.util.Map.of("question", "질문2", "options", Arrays.asList(
                        java.util.Map.of("id", 1, "text", "옵션1"),
                        java.util.Map.of("id", 2, "text", "옵션2"),
                        java.util.Map.of("id", 3, "text", "옵션3"),
                        java.util.Map.of("id", 4, "text", "옵션4")
                )));
        when(objectMapper.readValue(quiz3.getQuizContent(), java.util.Map.class))
                .thenReturn(java.util.Map.of("question", "세 번째 질문", "options", Arrays.asList(
                        java.util.Map.of("id", 1, "text", "옵션1"),
                        java.util.Map.of("id", 2, "text", "옵션2"),
                        java.util.Map.of("id", 3, "text", "옵션3"),
                        java.util.Map.of("id", 4, "text", "옵션4")
                )));
        when(objectMapper.readValue(quiz4.getQuizContent(), java.util.Map.class))
                .thenReturn(java.util.Map.of("question", "네 번째 질문", "options", Arrays.asList(
                        java.util.Map.of("id", 1, "text", "답1"),
                        java.util.Map.of("id", 2, "text", "답2"),
                        java.util.Map.of("id", 3, "text", "답3"),
                        java.util.Map.of("id", 4, "text", "답4")
                )));
        
        // when
        QuizResponseDto result = quizService.getQuizzesByArticleId(articleId);
        
        // then
        assertNotNull(result);
        assertEquals(4, result.getQuizContent().size());
        
        // 모든 퀴즈 ID가 unique한지 확인
        List<Long> quizIds = result.getQuizContent().stream()
                .map(QuizResponseDto.QuizContentDto::getQuizId)
                .distinct()
                .toList();
        assertEquals(4, quizIds.size());
        
        // Mock 메소드 호출 검증
        verify(objectMapper, times(4)).readValue(anyString(), eq(java.util.Map.class));
    }
} 