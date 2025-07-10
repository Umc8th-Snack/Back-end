package umc.snack.domain.user.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class UserClicksDto {
    private Long clickId;
    private Long userId;
    private Long articleId;
}