package umc.snack.domain.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QuizReportDto {
    @NotNull private Long userId;
    @NotNull private Long articleId;
    @Builder.Default
    private Boolean reported = true;
    @Size(max = 1000)
    private String reason;
}