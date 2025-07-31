package umc.snack.domain.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.domain.user.entity.User;
import umc.snack.global.BaseEntity;

@Entity
@Table(name = "social_login", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "provider"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SocialLogin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_login_id")
    private Long socialLoginId;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(length = 20)
    private String provider;

    @Column(name = "provider_social_id", length = 255)
    private String providerSocialId;

    @Column(name = "access_token", length = 255)
    private String accessToken;

    @Column(name = "refresh_token", length = 255)
    private String refreshToken;
}