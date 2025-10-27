package umc.snack.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.snack.domain.user.entity.UserClicks;

import java.time.LocalDateTime;
import java.util.List;

public interface UserClickRepository extends JpaRepository<UserClicks, Long> {
    List<UserClicks> findByUserIdAndCreatedAtAfter(Long userId, LocalDateTime createdAt);
    List<UserClicks> findTop15ByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByUserIdAndArticleId(Long userId, Long articleId);
}
