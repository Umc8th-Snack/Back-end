package umc.snack.service.report;

import umc.snack.domain.report.dto.QuizReportRequestDto;
import umc.snack.domain.report.dto.QuizReportResponseDto;

public interface QuizReportService {
    QuizReportResponseDto createReport(QuizReportRequestDto requestDto, Long userIdFromToken);
}


