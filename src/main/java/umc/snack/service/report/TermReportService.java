package umc.snack.service.report;

import umc.snack.domain.report.dto.TermReportRequestDto;
import umc.snack.domain.report.dto.TermReportResponseDto;

public interface TermReportService {
    TermReportResponseDto createReport(Long articleId, TermReportRequestDto requestDto, Long userIdFromToken);

    boolean hasReported(Long articleId, Long userIdFromToken);
}


