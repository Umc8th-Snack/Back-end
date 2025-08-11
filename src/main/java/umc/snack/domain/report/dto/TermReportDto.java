package umc.snack.domain.report.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class TermReportDto {
    @NotNull
    private Long userId;
    @NotNull
    private Long articleId;
    @Builder.Default
    private Boolean reported = true;
    @Size(max = 1000)
    private String reason;
}