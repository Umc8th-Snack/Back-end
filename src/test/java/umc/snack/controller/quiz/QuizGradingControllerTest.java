package umc.snack.controller.quiz;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.common.exception.GlobalExceptionHandler;
import umc.snack.domain.quiz.dto.QuizGradingRequestDto;
import umc.snack.domain.quiz.dto.QuizGradingResponseDto;
import umc.snack.service.quiz.QuizService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("퀴즈 채점 컨트롤러 테스트")
class QuizGradingControllerTest {

    @Mock
    private QuizService quizService;

    @InjectMocks
    private QuizController quizController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(quizController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("성공: 퀴즈 채점 성공 - 모든 정답")
    void gradeQuizzes_Success_AllCorrect() throws Exception {
        // given
        Long articleId = 1L;
        
        QuizGradingRequestDto requestDto = new QuizGradingRequestDto(Arrays.asList(
                new QuizGradingRequestDto.SubmittedAnswer(1L, 2),
                new QuizGradingRequestDto.SubmittedAnswer(2L, 1),
                new QuizGradingRequestDto.SubmittedAnswer(3L, 0),
                new QuizGradingRequestDto.SubmittedAnswer(4L, 3)
        ));

        List<QuizGradingResponseDto.QuizGradingDetail> details = Arrays.asList(
                QuizGradingResponseDto.QuizGradingDetail.builder()
                        .quizId(1L).isCorrect(true).submitted_answer(2).answer_index(2)
                        .description("정답에 대한 설명1").build(),
                QuizGradingResponseDto.QuizGradingDetail.builder()
                        .quizId(2L).isCorrect(true).submitted_answer(1).answer_index(1)
                        .description("정답에 대한 설명2").build(),
                QuizGradingResponseDto.QuizGradingDetail.builder()
                        .quizId(3L).isCorrect(true).submitted_answer(0).answer_index(0)
                        .description("정답에 대한 설명3").build(),
                QuizGradingResponseDto.QuizGradingDetail.builder()
                        .quizId(4L).isCorrect(true).submitted_answer(3).answer_index(3)
                        .description("정답에 대한 설명4").build()
        );

        QuizGradingResponseDto mockResponse = QuizGradingResponseDto.builder()
                .details(details)
                .build();

        when(quizService.gradeQuizzes(anyLong(), any(QuizGradingRequestDto.class)))
                .thenReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/articles/{articleId}/quizzes/grade", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("QUIZ_7501"))
                .andExpect(jsonPath("$.message").value("퀴즈 채점이 완료되었습니다"))
                .andExpect(jsonPath("$.correctCount").value(4))
                .andExpect(jsonPath("$.result.details").isArray())
                .andExpect(jsonPath("$.result.details").isNotEmpty())
                .andExpect(jsonPath("$.result.details[0].quizId").value(1))
                .andExpect(jsonPath("$.result.details[0].isCorrect").value(true))
                .andExpect(jsonPath("$.result.details[0].submitted_answer").value(2))
                .andExpect(jsonPath("$.result.details[0].answer_index").value(2));
    }

    @Test
    @DisplayName("성공: 퀴즈 채점 성공 - 일부 정답")
    void gradeQuizzes_Success_PartialCorrect() throws Exception {
        // given
        Long articleId = 1L;
        
        QuizGradingRequestDto requestDto = new QuizGradingRequestDto(Arrays.asList(
                new QuizGradingRequestDto.SubmittedAnswer(1L, 2),
                new QuizGradingRequestDto.SubmittedAnswer(2L, 3), // 오답
                new QuizGradingRequestDto.SubmittedAnswer(3L, 0),
                new QuizGradingRequestDto.SubmittedAnswer(4L, 1)  // 오답
        ));

        List<QuizGradingResponseDto.QuizGradingDetail> details = Arrays.asList(
                QuizGradingResponseDto.QuizGradingDetail.builder()
                        .quizId(1L).isCorrect(true).submitted_answer(2).answer_index(2)
                        .description("정답에 대한 설명1").build(),
                QuizGradingResponseDto.QuizGradingDetail.builder()
                        .quizId(2L).isCorrect(false).submitted_answer(3).answer_index(1)
                        .description("정답에 대한 설명2").build(),
                QuizGradingResponseDto.QuizGradingDetail.builder()
                        .quizId(3L).isCorrect(true).submitted_answer(0).answer_index(0)
                        .description("정답에 대한 설명3").build(),
                QuizGradingResponseDto.QuizGradingDetail.builder()
                        .quizId(4L).isCorrect(false).submitted_answer(1).answer_index(3)
                        .description("정답에 대한 설명4").build()
        );

        QuizGradingResponseDto mockResponse = QuizGradingResponseDto.builder()
                .details(details)
                .build();

        when(quizService.gradeQuizzes(anyLong(), any(QuizGradingRequestDto.class)))
                .thenReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/articles/{articleId}/quizzes/grade", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correctCount").value(2))
                .andExpect(jsonPath("$.result.details[1].isCorrect").value(false))
                .andExpect(jsonPath("$.result.details[3].isCorrect").value(false));
    }

    @Test
    @DisplayName("실패: 존재하지 않는 기사 ID")
    void gradeQuizzes_ArticleNotFound() throws Exception {
        // given
        Long articleId = 999L;
        QuizGradingRequestDto requestDto = new QuizGradingRequestDto(Arrays.asList(
                new QuizGradingRequestDto.SubmittedAnswer(1L, 2)
        ));

        when(quizService.gradeQuizzes(anyLong(), any(QuizGradingRequestDto.class)))
                .thenThrow(new CustomException(ErrorCode.QUIZ_7601));

        // when & then
        mockMvc.perform(post("/api/articles/{articleId}/quizzes/grade", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("실패: 기사에 퀴즈가 없음")
    void gradeQuizzes_NoQuizzesFound() throws Exception {
        // given
        Long articleId = 1L;
        QuizGradingRequestDto requestDto = new QuizGradingRequestDto(Arrays.asList(
                new QuizGradingRequestDto.SubmittedAnswer(1L, 2)
        ));

        when(quizService.gradeQuizzes(anyLong(), any(QuizGradingRequestDto.class)))
                .thenThrow(new CustomException(ErrorCode.QUIZ_7602));

        // when & then
        mockMvc.perform(post("/api/articles/{articleId}/quizzes/grade", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("실패: 유효하지 않은 퀴즈 ID")
    void gradeQuizzes_InvalidQuizId() throws Exception {
        // given
        Long articleId = 1L;
        QuizGradingRequestDto requestDto = new QuizGradingRequestDto(Arrays.asList(
                new QuizGradingRequestDto.SubmittedAnswer(999L, 2) // 존재하지 않는 퀴즈 ID
        ));

        when(quizService.gradeQuizzes(anyLong(), any(QuizGradingRequestDto.class)))
                .thenThrow(new CustomException(ErrorCode.QUIZ_7603));

        // when & then
        mockMvc.perform(post("/api/articles/{articleId}/quizzes/grade", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
} 