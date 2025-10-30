package umc.snack.service.feed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.snack.converter.feed.UserPreferenceConverter;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.article.entity.ArticleCategory;
import umc.snack.domain.feed.entity.Category;
import umc.snack.domain.user.dto.UserCategoryScoreDto;
import umc.snack.domain.user.entity.UserClicks;
import umc.snack.domain.user.entity.UserScrap;
import umc.snack.repository.user.UserClickRepository;
import umc.snack.repository.scrap.UserScrapRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserPreferenceServiceTest {

    // @InjectMocks는 생성자 주입을 사용하므로, Converter도 주입 대상에 포함시켜야 합니다.
    // 여기서는 간단하게 Mock으로 처리합니다.
    @Mock
    private UserPreferenceConverter userPreferenceConverter;

    @InjectMocks
    private UserPreferenceService userPreferenceService;

    @Mock
    private UserScrapRepository userScrapRepository;

    @Mock
    private UserClickRepository userClickRepository;

    @Test
    @DisplayName("사용자 행동 로그 기반 카테고리 선호도 점수 계산 테스트")
    void calculateCategoryScoresTest() {
        // given: 테스트를 위한 가짜 데이터 설정
        Long testUserId = 1L;

        Category itCategory = Category.builder().categoryId(1L).categoryName("IT/과학").build();
        Category economyCategory = Category.builder().categoryId(2L).categoryName("경제").build();

        // 행동 데이터 설정: IT 스크랩 2번, 경제 클릭 1번
        List<UserScrap> scraps = List.of(
                createMockScrapWithCategory(itCategory),
                createMockScrapWithCategory(itCategory)
        );
        List<UserClicks> clicks = List.of(
                createMockClickWithCategory(economyCategory)
        );

        // Repository가 올바른 메서드를 호출받으면 위에서 만든 가짜 데이터를 반환하도록 설정
        when(userScrapRepository.findByUserIdAndCreatedAtAfter(anyLong(), any(LocalDateTime.class)))
                .thenReturn(scraps);
        when(userClickRepository.findByUserIdAndCreatedAtAfter(anyLong(), any(LocalDateTime.class)))
                .thenReturn(clicks);

        // Converter가 호출될 때의 동작도 정의해줍니다. (실제 로직을 테스트하기 위함)
        // 이 부분은 실제 UserPreferenceConverter의 로직을 그대로 가져와서 테스트용으로 만듭니다.
        when(userPreferenceConverter.toUserCategoryScoreDto(anyLong(), any(Category.class), any(int[].class), any(Double.class), any(Double.class), any(Double.class)))
                .thenAnswer(invocation -> {
                    Long userId = invocation.getArgument(0);
                    Category category = invocation.getArgument(1);
                    int[] counts = invocation.getArgument(2);
                    double totalScraps = invocation.getArgument(3);
                    double totalClicks = invocation.getArgument(4);
                    // ... 실제 컨버터 로직과 동일하게 점수 계산 ...
                    float scrapScore = (totalScraps == 0) ? 0 : (float) (counts[0] / totalScraps);
                    float clickScore = (totalClicks == 0) ? 0 : (float) (counts[1] / totalClicks);
                    float behaviorScore = (float) ((scrapScore * 0.5) + (clickScore * 0.3));
                    return UserCategoryScoreDto.builder().userId(userId).categoryId(category.getCategoryId()).behaviorScore(behaviorScore).categoryId(category.getCategoryId()).build();
                });

        // when: 테스트하려는 메서드 호출
        List<UserCategoryScoreDto> result = userPreferenceService.calculateCategoryScores(testUserId);

        // then: 결과 검증
        assertThat(result).hasSize(2);

        UserCategoryScoreDto topCategory = result.get(0);
        assertThat(topCategory.getCategoryId()).isEqualTo(1L);
        assertThat(topCategory.getBehaviorScore()).isEqualTo(0.5f);

        UserCategoryScoreDto secondCategory = result.get(1);
        assertThat(secondCategory.getCategoryId()).isEqualTo(2L);
        assertThat(secondCategory.getBehaviorScore()).isEqualTo(0.3f);
    }

    // 테스트용 Mock 객체 생성을 위한 헬퍼 메서드 (내용 구현)
    private UserScrap createMockScrapWithCategory(Category category) {
        Article mockArticle = mock(Article.class);
        ArticleCategory mockArticleCategory = mock(ArticleCategory.class);
        UserScrap mockScrap = mock(UserScrap.class);

        when(mockArticleCategory.getCategory()).thenReturn(category);
        when(mockArticle.getArticleCategories()).thenReturn(List.of(mockArticleCategory));
        when(mockScrap.getArticle()).thenReturn(mockArticle);

        return mockScrap;
    }

    private UserClicks createMockClickWithCategory(Category category) {
        Article mockArticle = mock(Article.class);
        ArticleCategory mockArticleCategory = mock(ArticleCategory.class);
        UserClicks mockClick = mock(UserClicks.class);

        when(mockArticleCategory.getCategory()).thenReturn(category);
        when(mockArticle.getArticleCategories()).thenReturn(List.of(mockArticleCategory));
        when(mockClick.getArticle()).thenReturn(mockArticle);

        return mockClick;
    }
}
