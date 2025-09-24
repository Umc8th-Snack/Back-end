package umc.snack.domain.report.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
public class QuizReportRequestDto {
    // URL에서 articleId를 받으므로 DTO에서 제거
    // reason은 디폴트 값을 사용하므로 제거
}


