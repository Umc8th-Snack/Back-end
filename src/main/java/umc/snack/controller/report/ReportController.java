package umc.snack.controller.report;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import umc.snack.common.config.security.CustomUserDetails;
import umc.snack.common.dto.ApiResponse;
import umc.snack.domain.report.dto.QuizReportRequestDto;
import umc.snack.domain.report.dto.QuizReportResponseDto;
import umc.snack.service.report.QuizReportService;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Report", description = "신고 API")
public class ReportController {

    private final QuizReportService quizReportService;

    @Operation(summary = "퀴즈 해설 신고하기", description = "사용자가 특정 기사(퀴즈 해설)에 대해 신고를 생성합니다. 한 사용자는 같은 기사에 대해 한 번만 신고할 수 있습니다.")
    @PostMapping("/quiz")
    public ResponseEntity<ApiResponse<QuizReportResponseDto>> reportQuiz(
            @Valid @RequestBody QuizReportRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        Long userIdFromToken = customUserDetails != null ? customUserDetails.getUserId() : null;
        QuizReportResponseDto result = quizReportService.createReport(requestDto, userIdFromToken);

        ApiResponse<QuizReportResponseDto> response = ApiResponse.onSuccess(
                "201",
                "퀴즈 해설 신고가 접수되었습니다",
                result
        );
        return ResponseEntity.status(201).body(response);
    }
}


