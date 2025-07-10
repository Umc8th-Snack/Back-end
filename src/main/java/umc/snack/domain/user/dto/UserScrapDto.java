package umc.snack.domain.user.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class UserScrapDto {
    private Long scrapId;
    private Long articleId;
    private Long userId;
}