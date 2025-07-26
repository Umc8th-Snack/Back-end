package umc.snack.service.share;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.article.entity.Article;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.domain.share.dto.ShareResultDto;
import umc.snack.domain.share.dto.SharedArticleContentDto;
import umc.snack.domain.share.entity.ArticleShare;
import umc.snack.repository.article.ArticleShareRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ShareServiceImpl implements ShareService {

    private final ArticleRepository articleRepository;
    private final ArticleShareRepository articleShareRepository;

    @Override
    public ShareResultDto createShareLink(Long articleId, Long userId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new CustomException(ErrorCode.SHARE_6601));

        // 공유 가능 여부 판단 (예: 비공개거나 승인 안 됐으면 안됨)
        if (!isSharable(article)) {
            throw new CustomException(ErrorCode.SHARE_6602);
        }

        String uuid = UUID.randomUUID().toString();

        ArticleShare articleShare = ArticleShare.builder()
                .shareId(System.currentTimeMillis()) // 복합키니까 유일값 필요
                .articleId(article.getArticleId())
                .article(article)
                .shareUuid(uuid)
                .build();

        articleShareRepository.save(articleShare);

        String sharedUrl = "https://snack.com/share/" + uuid;
        return new ShareResultDto(uuid, sharedUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public SharedArticleContentDto getSharedArticleByUuid(String uuid) {
        ArticleShare articleShare = articleShareRepository.findByShareUuid(uuid)
                .orElseThrow(() -> new CustomException(ErrorCode.SHARE_6601));

        Article article = articleShare.getArticle();

        if (!isSharable(article)) {
            throw new CustomException(ErrorCode.SHARE_6602);
        }

        String categoryName = article.getArticleCategories().isEmpty()
                ? "미지정"
                : article.getArticleCategories().get(0).getCategory().getCategoryName();


        return new SharedArticleContentDto(
                article.getArticleId(),
                article.getTitle(),
                article.getSummary(),
                article.getPublishedAt(),
                article.getArticleUrl(),
                categoryName
        );
    }

    private boolean isSharable(Article article) {
        //  게시일이 현재보다 미래인 경우: 아직 공개 안 된 기사
        if (article.getPublishedAt() != null && article.getPublishedAt().isAfter(LocalDateTime.now())) {
            return false;
        }

        //  (추후) 비공개 상태일 경우
        // ex) if (article.getVisibility() == Visibility.PRIVATE) return false;

        //  (추후) 특정 카테고리(예: 정치)인 경우
        // boolean hasBlockedCategory = article.getArticleCategories().stream()
        //         .map(ac -> ac.getCategory().getCategoryName())
        //         .anyMatch(name -> name.equals("정치"));
        // if (hasBlockedCategory) return false;

        return true;
    }

}