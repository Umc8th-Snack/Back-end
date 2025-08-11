package umc.snack.domain.article.dto;

import lombok.Builder;
import lombok.Getter;
import umc.snack.domain.article.entity.Article; // Article 엔티티 import

@Getter
public class RelatedArticleDto {

    private Long articleId; // 엔티티의 articleId 타입이 Long이므로 맞춰줍니다.
    private String title;
    private String imageUrl; // API 명세에서는 thumbnailUrl, 엔티티에서는 imageUrl

    @Builder
    public RelatedArticleDto(Long articleId, String title, String imageUrl) {
        this.articleId = articleId;
        this.title = title;
        this.imageUrl = imageUrl;
    }

    // Article 엔티티를 DTO로 변환하는 정적 메소드 (수정됨)
    public static RelatedArticleDto fromEntity(Article article) {
        return RelatedArticleDto.builder()
                .articleId(article.getArticleId()) // article.getArticleId() 사용
                .title(article.getTitle())
                .imageUrl(article.getImageUrl()) // article.getImageUrl() 사용
                .build();
    }
}
