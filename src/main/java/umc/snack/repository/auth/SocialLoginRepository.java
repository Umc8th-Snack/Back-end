package umc.snack.repository.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.snack.domain.auth.entity.SocialLogin;

import java.util.Optional;

public interface SocialLoginRepository extends JpaRepository<SocialLogin, Long> {
    
    Optional<SocialLogin> findByUserIdAndProvider(Long userId, String provider);
    
    void deleteByUserId(Long userId);
    
    boolean existsByUserIdAndProvider(Long userId, String provider);
}

