package umc.snack.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.global.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    // 소셜 전용 계정은 패스워드가 없을 수 있음
    @Column(nullable = true, length = 255)
    private String password;

    @Column(unique = true, nullable = false, length = 255)
    private String nickname;

    // 프로필의 링크
    @Column(name = "profile_url")
    private String profileUrl;

    // 프로필 사진의 링크
    @Column(name = "profile_image")
    private String profileImage;

    private String introduction;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Column(name = "delete_at")
    private java.time.LocalDateTime deleteAt;

    public void withdraw() {
                this.status = Status.DELETED;
                this.deleteAt = LocalDateTime.now();
    }

    public void updateUserInfo(String nickname, String profileImage, String introduction) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (profileImage != null) {
            this.profileImage = profileImage;
        }
        if (introduction != null) {
            this.introduction = introduction;
        }
    }

    // 이메일 변경
    public void changeEmail(String newEmail) {
        this.email = newEmail;
    }

    // 비밀번호 변경
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.ROLE_USER;

    public enum LoginType { LOCAL, GOOGLE }

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "login_type", length = 20)
    private LoginType loginType = LoginType.LOCAL;

    // 소셜 전용 계정 판별
    public boolean isSocialOnly() {
        return this.loginType != LoginType.LOCAL;
    }
    public void setLoginType(LoginType loginType) {
        this.loginType = loginType;
    }

    public enum Status {
        ACTIVE,
        DELETED
    }

    public enum Role {
        ROLE_USER,
        ROLE_ADMIN
    }

}
