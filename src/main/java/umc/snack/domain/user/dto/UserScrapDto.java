package umc.snack.domain.user.dto;

import lombok.*;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.user.entity.UserScrap;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserScrapDto {

    private Long scrapId;
    private Long articleId;
    private String title;           // 기사 제목
    private String summaryPreview;  // 기사 요약 앞 100자
    private LocalDateTime publishedAt;

    public static UserScrapDto from(UserScrap userScrap) {
        Article article = userScrap.getArticle();
        if (article == null) {
            throw new CustomException(ErrorCode.ARTICLE_9105_GET);
        }

        String fullSummary = article.getSummary(); // 요약 전체
        String preview;
        // if-else 로 읽기 쉽게 분리
        if (fullSummary != null && fullSummary.length() > 100) {
            preview = fullSummary.substring(0, 100) + "...";
        } else {
            preview = (fullSummary != null ? fullSummary : "");
        }

        return UserScrapDto.builder()
                .scrapId(userScrap.getScrapId())
                .articleId(article.getArticleId())
                .title(article.getTitle())
                .summaryPreview(preview)
                .publishedAt(article.getPublishedAt())
                .build();
    }
}