package umc.snack.repository.memo;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.memo.entity.Memo;
import umc.snack.domain.user.entity.User;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface MemoRepository extends JpaRepository<Memo, Long> {
    Page<Memo> findByUser(User user, Pageable pageable);
    void deleteAllByUser_UserId(Long userId);

    Optional<Memo> findByUserAndArticle(User user, Article article);
}
