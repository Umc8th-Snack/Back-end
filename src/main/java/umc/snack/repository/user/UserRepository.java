package umc.snack.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.snack.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    User findByEmail(String email);
    User findByNickname(String nickname);
}
