package umc.snack.domain.term.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class TermDto {
    private Long termId;
    private String word;
    private String definition;
}