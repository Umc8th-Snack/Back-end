package umc.snack.domain.article.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class ArticleDto {
    private Long articleId;
    private String title;
    private String summary;
    private String publishedAt;
    private String articleUrl;    // 외부 네이버 뉴스 URL
    private String snackUrl;      // "/articles/{id}" (스낵 서비스 내 상세 페이지 경로)
    private String category;  // 카테고리 이름
}