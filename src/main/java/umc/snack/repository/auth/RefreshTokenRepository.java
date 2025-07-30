package umc.snack.repository.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.snack.domain.auth.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    boolean existsByRefreshToken(String refreshToken);
    boolean existsByUserId(Long userId);
    void deleteByRefreshToken(String refreshToken);
    void deleteByUserId(Long userId);
    Optional<RefreshToken> findByRefreshToken(String refreshToken);

}
