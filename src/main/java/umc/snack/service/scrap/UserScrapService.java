package umc.snack.service.scrap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import umc.snack.domain.user.entity.UserScrap;

public interface UserScrapService {
    void addScrap(Long userId, Long articleId);
    void cancelScrap(Long userId, Long articleId);
    Page<UserScrap> getScrapList(Long userId, Pageable pageable);
    boolean isScrapped(Long userId, Long articleId);
    String getArticleUrlByScrapId(Long scrapId);
}
