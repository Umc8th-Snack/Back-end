package umc.snack.domain.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class QuizReportDto {
    private Long userId;
    private Long articleId;
    private Boolean reported;
    private String reason;
}