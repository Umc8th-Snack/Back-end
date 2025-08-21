package umc.snack.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.snack.domain.user.entity.VerificationCode;

import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    // 이메일로 인증 코드를 찾기 위한 메서드
    Optional<VerificationCode> findByEmail(String email);
}
