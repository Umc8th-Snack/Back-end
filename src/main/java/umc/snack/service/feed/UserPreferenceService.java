package umc.snack.service.feed;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.snack.converter.feed.UserPreferenceConverter;
import umc.snack.domain.feed.entity.Category;
import umc.snack.domain.user.dto.UserCategoryScoreDto;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.user.UserClickRepository;
import umc.snack.repository.scrap.UserScrapRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPreferenceService {
    private final UserScrapRepository userScrapRepository;
    private final UserClickRepository userClickRepository;
    private final ArticleRepository articleRepository;
    private final UserPreferenceConverter userPreferenceConverter;

    private static final double WEIGHT_SCRAP = 0.5;
    private static final double WEIGHT_CLICK = 0.3;
    private static final double WEIGHT_SEARCH = 0.2;
    private static final int RECENT_DAYS = 30;

    public List<UserCategoryScoreDto> calculateCategoryScores(Long userId) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RECENT_DAYS);

        // category 별 행동 횟수 집계
        Map<Category, int[]> categoryActionCounts = new HashMap<>();

        userScrapRepository.findByUserIdAndCreatedAtAfter(userId, threshold).forEach(scrap -> {
            scrap.getArticle().getArticleCategories().forEach(ac -> {
                categoryActionCounts.computeIfAbsent(ac.getCategory(), k -> new int[3])[0]++;
            });
        });

        userClickRepository.findByUserIdAndCreatedAtAfter(userId, threshold).forEach(scrap -> {
            scrap.getArticle().getArticleCategories().forEach(ac -> {
                categoryActionCounts.computeIfAbsent(ac.getCategory(), k -> new int [3])[1]++;
            });
        });

        // 전체 행동 횟수 계산
        double totalScraps = categoryActionCounts.values().stream().mapToInt(c -> c[0]).sum();
        double totalClicks = categoryActionCounts.values().stream().mapToInt(c -> c[1]).sum();
        double totalSearches = categoryActionCounts.values().stream().mapToInt(c -> c[2]).sum();

        return categoryActionCounts.entrySet().stream()
                .map(categoryEntry -> userPreferenceConverter.toUserCategoryScoreDto(
                        userId,
                        categoryEntry.getKey(),
                        categoryEntry.getValue(),
                        totalScraps,
                        totalClicks,
                        totalSearches
                )) .sorted(Comparator.comparing(UserCategoryScoreDto::getBehaviorScore).reversed())
                .limit(3)
                .collect(Collectors.toList());
    }
}
