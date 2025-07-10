package umc.snack.domain.auth.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class SocialLoginDto {
    private Long socialLoginId;
    private Long userId;
    private String provider;
    private String providerSocialId;
    private String accessToken;
    private String refreshToken;
}
