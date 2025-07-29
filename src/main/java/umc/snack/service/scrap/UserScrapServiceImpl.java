package umc.snack.service.scrap;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.user.entity.User;
import umc.snack.domain.user.entity.UserScrap;
import umc.snack.repository.article.ArticleRepository;
import umc.snack.repository.scrap.UserScrapRepository;
import umc.snack.repository.user.UserRepository;

import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class UserScrapServiceImpl implements UserScrapService {

    private final UserScrapRepository userScrapRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void addScrap(Long userId, Long articleId) {
        if (userScrapRepository.existsByUserIdAndArticleId(userId, articleId)) {
            throw new CustomException(ErrorCode.SCRAP_6101);
        }

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCRAP_6103));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_2622));

        UserScrap scrap = UserScrap.builder()
                .user(user)
                .article(article)
                .build();

        userScrapRepository.save(scrap);
    }

    @Override
    @Transactional
    public void cancelScrap(Long userId, Long articleId) {
        UserScrap scrap = userScrapRepository.findByUserIdAndArticleId(userId, articleId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCRAP_6105));
        userScrapRepository.delete(scrap);
    }

    @Override
    public Page<UserScrap> getScrapList(Long userId, Pageable pageable) {
        //스크랩 최신순으로 정렬
        Pageable sortedPageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                org.springframework.data.domain.Sort.by("createdAt").descending()
        );
        return userScrapRepository.findAllByUserId(userId, sortedPageable);
    }

    @Override
    public boolean isScrapped(Long userId, Long articleId) {
        return userScrapRepository.existsByUserIdAndArticleId(userId, articleId);
    }

    @Override
    public String getArticleUrlByScrapIdAndUserId(Long scrapId, Long userId) {
        UserScrap userScrap = userScrapRepository.findById(scrapId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCRAP_6105)); // 스크랩 없음

        if (!userScrap.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.SCRAP_6106); // 권한 없음
        }

        return "/api/articles/" + userScrap.getArticle().getArticleId(); // 내부 URI
    }
}
