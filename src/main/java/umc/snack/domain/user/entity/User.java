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

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false, length = 255)
    private String nickname;

    @Column(name = "profile_url")
    private String profileUrl;

    @Column(name = "profile_image")
    private String profileImage;

    private String introduction;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Column(name = "delete_at")
    private java.time.LocalDateTime deleteAt;

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setDeleteAt(LocalDateTime deleteAt) {
        this.deleteAt = deleteAt;
    }

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.ROLE_USER;

    public enum Status {
        ACTIVE,
        DELETED
    }

    public enum Role {
        ROLE_USER,
        ROLE_ADMIN
    }

}
