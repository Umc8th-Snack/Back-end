package umc.snack.service.scrap;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.user.entity.UserScrap;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.scrap.UserScrapRepository;

import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class UserScrapServiceImpl implements UserScrapService {

    private final UserScrapRepository userScrapRepository;
    private final ArticleRepository articleRepository;

    @Override
    public void addScrap(Long userId, Long articleId) {
        if (userScrapRepository.existsByUserIdAndArticleId(userId, articleId)) {
            throw new CustomException(ErrorCode.SCRAP_6101);
        }

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCRAP_6103));

        UserScrap scrap = UserScrap.builder()
                .userId(userId)
                .articleId(articleId)
                .build();

        userScrapRepository.save(scrap);
    }

    @Override
    public void cancelScrap(Long userId, Long articleId) {
        UserScrap scrap = userScrapRepository.findByUserIdAndArticleId(userId, articleId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCRAP_6105));
    }

    @Override
    public Page<UserScrap> getScrapList(Long userId, Pageable pageable) {
        return userScrapRepository.findAllByUserId(userId, pageable);
    }

    @Override
    public boolean isScrapped(Long userId, Long articleId) {
        return userScrapRepository.existsByUserIdAndArticleId(userId, articleId);
    }

    @Override
    public String getArticleUrlByScrapId(Long scrapId) {
        return userScrapRepository.findById(scrapId)
                .map(scrap -> "/api/articles/" + scrap.getArticleId())  // 내부 상세 페이지 URI 구성
                .orElseThrow(() -> new CustomException(ErrorCode.SCRAP_6107));
    }
}
