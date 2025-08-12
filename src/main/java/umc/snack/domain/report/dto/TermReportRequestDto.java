package umc.snack.domain.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermReportRequestDto {
    private Long articleId;
//    private Boolean reported;
    private String reason;
}


