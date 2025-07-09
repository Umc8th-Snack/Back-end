package umc.snack.domain.user.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class UserCategoryScoreDto {
    private Long scoreId;
    private Long userId;
    private Long categoryId;
    private Float scrapScore;
    private Float clickScore;
    private Float searchScore;
    private Float behaviorScore;
}
