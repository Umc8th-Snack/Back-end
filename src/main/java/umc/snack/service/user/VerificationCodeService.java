package umc.snack.service.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import umc.snack.domain.user.entity.VerificationCode;
import umc.snack.repository.user.VerificationCodeRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class VerificationCodeService {
    private final VerificationCodeRepository verificationCodeRepository;

    public void saveCode(String email, String code) {
        // 5분 후 만료 시간 설정
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        // 이메일로 기존 코드가 있는지 확인
        VerificationCode verificationCode = verificationCodeRepository.findByEmail(email)
                .orElse(null);

        if (verificationCode != null) {
            // 기존 코드가 있다면 코드와 만료 시간만 업데이트
            verificationCode.updateCode(code, expiresAt);
        } else {
            // 기존 코드가 없다면 새로 생성하여 저장
            verificationCode = VerificationCode.builder()
                    .email(email)
                    .code(code)
                    .expiresAt(expiresAt)
                    .build();
            verificationCodeRepository.save(verificationCode);
        }
    }
}
