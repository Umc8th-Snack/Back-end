package umc.snack.controller.report;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import umc.snack.common.config.security.CustomUserDetails;
import umc.snack.common.dto.ApiResponse;
import umc.snack.domain.report.dto.QuizReportRequestDto;
import umc.snack.domain.report.dto.QuizReportResponseDto;
import umc.snack.domain.report.dto.TermReportRequestDto;
import umc.snack.domain.report.dto.TermReportResponseDto;
import umc.snack.service.report.QuizReportService;
import umc.snack.service.report.TermReportService;

@RestController
@RequestMapping("/api/articles/{articleId}/reports")
@RequiredArgsConstructor
@Tag(name = "Report", description = "신고 API")
public class ReportController {

    private final QuizReportService quizReportService;
    private final TermReportService termReportService;

    @Operation(summary = "퀴즈 해설 신고하기", description = "사용자가 특정 기사(퀴즈 해설)에 대해 신고를 생성합니다. 한 사용자는 같은 기사에 대해 한 번만 신고할 수 있습니다.")
    @PostMapping("/quiz")
    public ResponseEntity<ApiResponse<QuizReportResponseDto>> reportQuiz(
            @PathVariable("articleId") Long articleId,
            @Valid @RequestBody QuizReportRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        Long userIdFromToken = customUserDetails.getUserId();
        QuizReportResponseDto result = quizReportService.createReport(articleId, requestDto, userIdFromToken);

        ApiResponse<QuizReportResponseDto> response = ApiResponse.onSuccess(
                "201",
                "퀴즈 해설 신고가 접수되었습니다",
                result
        );
        return ResponseEntity.status(201).body(response);
    }

    @Operation(summary = "퀴즈 신고 상태 조회", description = "사용자가 해당 기사 퀴즈 해설을 이미 신고했는지 여부를 반환합니다.")
    @GetMapping("/quiz/status")
    public ResponseEntity<ApiResponse<Object>> getQuizReportStatus(
            @PathVariable("articleId") Long articleId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        Long userIdFromToken = customUserDetails.getUserId();
        boolean reported = quizReportService.hasReported(articleId, userIdFromToken);

        // 응답 구조 생성
        final boolean isReported = reported; // 변수명 충돌 방지
        Object result = new Object() {
            public final boolean reported = isReported;
            public final String status = isReported ? "REPORTED" : "NOT_REPORTED";
            public final String actionLabel = isReported ? "신고 완료" : "신고 가능";
        };

        ApiResponse<Object> response = ApiResponse.onSuccess(
                "200",
                "퀴즈 신고 상태 조회 성공",
                result
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "용어 신고하기", description = "기사에 포함된 용어 설명에 대해 신고를 생성합니다. 한 사용자는 같은 기사에 대해 한 번만 신고할 수 있습니다.")
    @PostMapping("/term")
    public ResponseEntity<ApiResponse<TermReportResponseDto>> reportTerm(
            @PathVariable("articleId") Long articleId,
            @Valid @RequestBody TermReportRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        Long userIdFromToken = customUserDetails.getUserId();
        TermReportResponseDto result = termReportService.createReport(articleId, requestDto, userIdFromToken);

        ApiResponse<TermReportResponseDto> response = ApiResponse.onSuccess(
                "201",
                "용어 신고가 접수되었습니다",
                result
        );
        return ResponseEntity.status(201).body(response);
    }

    @Operation(summary = "용어 신고 상태 조회", description = "사용자가 해당 기사 용어 설명을 이미 신고했는지 여부를 반환합니다.")
    @GetMapping("/term/status")
    public ResponseEntity<ApiResponse<Object>> getTermReportStatus(
            @PathVariable("articleId") Long articleId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        Long userIdFromToken = customUserDetails.getUserId();
        boolean reported = termReportService.hasReported(articleId, userIdFromToken);

        // 응답 구조 생성
        final boolean isReported = reported; // 변수명 충돌 방지
        Object result = new Object() {
            public final boolean reported = isReported;
            public final String status = isReported ? "REPORTED" : "NOT_REPORTED";
            public final String actionLabel = isReported ? "신고 완료" : "신고 가능";
        };

        ApiResponse<Object> response = ApiResponse.onSuccess(
                "200",
                "용어 신고 상태 조회 성공",
                result
        );
        return ResponseEntity.ok(response);
    }
}