package umc.snack.repository.scrap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import umc.snack.domain.user.entity.UserScrap;

import java.util.Optional;

public interface UserScrapRepository extends JpaRepository<UserScrap, Long> {

    boolean existsByUserIdAndArticleId(Long userId, Long articleId);

    Optional<UserScrap> findByUserIdAndArticleId(Long userId, Long articleId);

    Page<UserScrap> findAllByUserId(Long userId, Pageable pageable);
}
