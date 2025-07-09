package umc.snack.domain.user.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class SearchKeywordDto {
    private Long searchId;
    private Long userId;
    private String keyword;
}
