package umc.snack.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.snack.domain.user.SearchKeywordId;
import umc.snack.domain.user.entity.SearchKeyword;

import java.util.List;
import java.util.Optional;

public interface SearchKeywordRepository extends JpaRepository<SearchKeyword, Long> {

    Optional<SearchKeyword> findByUserIdAndKeyword(Long userId, String keyword);

    List<SearchKeyword> findTop10ByUserIdOrderByCreatedAtDesc(Long userId); // BaseEntity 상속 가정

    List<SearchKeyword> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
