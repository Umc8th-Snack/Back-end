package umc.snack.repository.feed;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import umc.snack.domain.feed.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByCategoryName(String categoryName);

    boolean existsByCategoryName(String categoryName);
}
