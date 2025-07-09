package umc.snack.domain.user.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class UserPreferCategoryDto {
    private Long userId;
    private Long categoryId;
}
