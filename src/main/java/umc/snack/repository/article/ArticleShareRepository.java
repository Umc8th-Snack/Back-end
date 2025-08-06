package umc.snack.repository.article;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.snack.domain.share.entity.ArticleShare;

import java.util.Optional;

public interface ArticleShareRepository extends JpaRepository<ArticleShare, Long> {
    Optional<ArticleShare> findByShareUuid(String shareUuid);
}