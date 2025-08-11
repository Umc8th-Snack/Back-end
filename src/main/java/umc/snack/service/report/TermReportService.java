package umc.snack.service.report;

import umc.snack.domain.report.dto.TermReportRequestDto;
import umc.snack.domain.report.dto.TermReportResponseDto;

public interface TermReportService {
    TermReportResponseDto createReport(TermReportRequestDto requestDto, Long userIdFromToken);
}


