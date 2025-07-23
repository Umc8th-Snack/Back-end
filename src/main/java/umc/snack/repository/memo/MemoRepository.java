package umc.snack.repository.memo;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.snack.domain.memo.entity.Memo;

public interface MemoRepository extends JpaRepository<Memo, Long> {
}
