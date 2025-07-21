package umc.snack.controller.quiz;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import umc.snack.common.dto.ApiResponse;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.quiz.dto.QuizResponseDto;
import umc.snack.domain.quiz.dto.QuizSubmissionRequestDto;
import umc.snack.service.quiz.QuizService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Quiz", description = "퀴즈 관련 API")
public class QuizController {

    private final QuizService quizService;

    @Operation(summary = "기사에 해당하는 퀴즈 조회", description = "특정 기사에 해당하는 퀴즈 4개를 반환하는 API입니다.")
    @GetMapping("/articles/{article_id}/quiz")
    public ResponseEntity<ApiResponse<QuizResponseDto>> getQuiz(@PathVariable("article_id") Long articleId) {

        QuizResponseDto quizResponse = quizService.getQuizzesByArticleId(articleId);

        ApiResponse<QuizResponseDto> response = ApiResponse.onSuccess(
                "QUIZ_7500",
                "기사 퀴즈 조회에 성공하였습니다.",
                quizResponse
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "퀴즈 제출", description = "퀴즈를 푼 뒤 정답 및 점수를 반환받는 API입니다.")
    @PostMapping("/quizzes/submit")
    public ResponseEntity<?> submitQuiz(
            @PathVariable Long quiz_id,
            @RequestBody QuizSubmissionRequestDto requestDto,
            @RequestBody Object submitDto) {
        // TODO: 개발 예정
        return ResponseEntity.ok("퀴즈 제출 API - 개발 예정 (quizId: " + quiz_id + ")");
    }
}