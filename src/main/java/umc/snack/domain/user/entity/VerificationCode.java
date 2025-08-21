package umc.snack.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiresAt; // 만료 시간

//    private boolean verified = false; // 인증 코드 확인 여부
//
//    public void setVerified() {
//        this.verified = true;
//    }

    @Builder
    public VerificationCode(String email, String code, LocalDateTime expiresAt) {
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    public void updateCode(String newCode, LocalDateTime newExpiresAt) {
        this.code = newCode;
        this.expiresAt = newExpiresAt;
    }
}
