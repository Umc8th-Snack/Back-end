package umc.snack.controller.quiz;

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
import umc.snack.domain.quiz.dto.QuizResponseDto;
import umc.snack.service.quiz.QuizService;

import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("퀴즈 컨트롤러 간단한 테스트")
class QuizControllerSimpleTest {

    @Mock
    private QuizService quizService;

    @InjectMocks
    private QuizController quizController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(quizController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("성공: 퀴즈 조회 성공")
    void getQuiz_Success() throws Exception {
        // given
        Long articleId = 1L;
        QuizResponseDto.QuizContentDto quiz1 = QuizResponseDto.QuizContentDto.builder()
                .quizId(1L)
                .question("테스트 질문1")
                .options(Arrays.asList("답1", "답2", "답3", "답4"))
                .build();

        QuizResponseDto.QuizContentDto quiz2 = QuizResponseDto.QuizContentDto.builder()
                .quizId(2L)
                .question("테스트 질문2")
                .options(Arrays.asList("답A", "답B", "답C", "답D"))
                .build();

        QuizResponseDto mockResponse = QuizResponseDto.builder()
                .quizContent(Arrays.asList(quiz1, quiz2))
                .build();

        when(quizService.getQuizzesByArticleId(articleId)).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/articles/{article_id}/quiz", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("QUIZ_7500"))
                .andExpect(jsonPath("$.message").value("기사 퀴즈 조회에 성공하였습니다."))
                .andExpect(jsonPath("$.result.quizContent").isArray())
                .andExpect(jsonPath("$.result.quizContent").isNotEmpty())
                .andExpect(jsonPath("$.result.quizContent[0].quizId").value(1))
                .andExpect(jsonPath("$.result.quizContent[0].question").value("테스트 질문1"))
                .andExpect(jsonPath("$.result.quizContent[1].quizId").value(2))
                .andExpect(jsonPath("$.result.quizContent[1].question").value("테스트 질문2"));
    }

    @Test
    @DisplayName("실패: 기사가 존재하지 않는 경우")
    void getQuiz_ArticleNotFound() throws Exception {
        // given
        Long articleId = 999L;
        when(quizService.getQuizzesByArticleId(articleId))
                .thenThrow(new CustomException(ErrorCode.QUIZ_7601));

        // when & then
        mockMvc.perform(get("/api/articles/{article_id}/quiz", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("QUIZ_7601"));
    }

//    @Test
//    @DisplayName("실패: 잘못된 형식의 article_id")
//    void getQuiz_InvalidArticleId() throws Exception {
//        // when & then
//        mockMvc.perform(get("/api/articles/{article_id}/quiz", "invalid")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON))
//                .andDo(print())
//                .andExpect(status().isBadRequest());
//    }

    @Test
    @DisplayName("실패: 서버 내부 오류")
    void getQuiz_InternalServerError() throws Exception {
        // given
        Long articleId = 3L;
        when(quizService.getQuizzesByArticleId(articleId))
                .thenThrow(new CustomException(ErrorCode.SERVER_5101));

        // when & then
        mockMvc.perform(get("/api/articles/{article_id}/quiz", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SERVER_5101"));
    }
} 