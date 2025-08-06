package umc.snack.converter.feed;

import org.springframework.stereotype.Component;
import umc.snack.domain.feed.entity.Category;
import umc.snack.domain.user.dto.UserCategoryScoreDto;

@Component
public class UserPreferenceConverter {
    private static final double WEIGHT_SCRAP = 0.5;
    private static final double WEIGHT_CLICK = 0.3;
    private static final double WEIGHT_SEARCH = 0.2;


    public UserCategoryScoreDto toUserCategoryScoreDto(Long userId, Category category,
                                                       int[] counts, double totalScraps,
                                                       double totalClicks, double totalSearches) {
        // 각 행동점수 0~1 사이로 정규화
        float scrapScore = (totalScraps == 0) ? 0 : (float) (counts[0] / totalScraps);
        float clickScore = (totalClicks == 0) ? 0 : (float) (counts[1] / totalClicks);
        float searchScore = (totalSearches == 0) ? 0 : (float) (counts[2] / totalSearches);

        float behaviorScore = (float) ((scrapScore * WEIGHT_SCRAP) + (clickScore * WEIGHT_CLICK) + (searchScore * WEIGHT_SEARCH));

        return UserCategoryScoreDto.builder()
                .userId(userId)
                .categoryId(category.getCategoryId())
                .scrapScore(scrapScore)
                .clickScore(clickScore)
                .searchScore(searchScore)
                .behaviorScore(behaviorScore)
                .build();
    }
}
