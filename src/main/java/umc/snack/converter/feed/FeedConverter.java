package umc.snack.converter.feed;

import org.springframework.stereotype.Component;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.feed.dto.ArticleInFeedDto;
import umc.snack.domain.feed.dto.IndividualArticleDto;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FeedConverter {
    public ArticleInFeedDto toArticleInFeedDto(String category, boolean hasNext, Long nextCursorId, List<Article> articles) {
        // 개별 기사 DTO 리스트로 변환
        List<IndividualArticleDto> individualArticles = articles.stream()
                .map(this::toIndividualArticleDto) // 아래의 개별 변환 메서드 호출
                .toList();

        return ArticleInFeedDto.builder()
                .category(category)
                .hasNext(hasNext)
                .nextCursorId(nextCursorId)
                .articles(individualArticles)
                .build();
    }

    // Article 엔티티 하나를 개별 기사 DTO(IndividualArticleDto)로 변환하는 메서드
    private IndividualArticleDto toIndividualArticleDto(Article article) {
        return IndividualArticleDto.builder()
                .articleId(article.getArticleId())
                .title(article.getTitle())
                .publishedAt(article.getPublishedAt())
                .imageUrl(article.getImageUrl())
                .categories(article.getArticleCategories().stream()
                        .map(ac -> ac.getCategory().getCategoryName())
                        .collect(Collectors.toList()))
                .build();
    }

}
