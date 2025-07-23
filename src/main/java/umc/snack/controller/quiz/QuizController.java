package umc.snack.controller.quiz;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import umc.snack.common.dto.ApiResponse;
import umc.snack.domain.quiz.dto.QuizGradingApiResponse;
import umc.snack.domain.quiz.dto.QuizGradingRequestDto;
import umc.snack.domain.quiz.dto.QuizGradingResponseDto;
import umc.snack.domain.quiz.dto.QuizResponseDto;
import umc.snack.domain.quiz.dto.QuizSubmissionRequestDto;
import umc.snack.service.quiz.QuizService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Quiz", description = "퀴즈 API")
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

    @Operation(summary = "퀴즈 채점", description = "사용자가 제출한 퀴즈 답안을 채점하고 결과를 반환하는 API입니다.")
    @PostMapping("/articles/{articleId}/quizzes/grade")
    public ResponseEntity<QuizGradingApiResponse> gradeQuizzes(
            @PathVariable("articleId") Long articleId,
            @RequestBody QuizGradingRequestDto requestDto,
            @AuthenticationPrincipal String userEmail) {

        QuizGradingResponseDto gradingResult = quizService.gradeQuizzes(articleId, requestDto);

        // 맞힌 문항 수 계산
        long correctCount = gradingResult.getDetails().stream()
                .mapToLong(detail -> detail.isCorrect() ? 1 : 0)
                .sum();

        // 응답 객체 구성 (요구사항에 맞는 형식)
        QuizGradingApiResponse response = QuizGradingApiResponse.builder()
                .isSuccess(true)
                .code("QUIZ_7501")
                .message("퀴즈 채점이 완료되었습니다")
                .correctCount(correctCount)
                .result(gradingResult)
                .build();

        return ResponseEntity.ok(response);
    }
}