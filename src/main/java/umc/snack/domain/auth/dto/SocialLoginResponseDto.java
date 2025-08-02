package umc.snack.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SocialLoginResponseDto {
    private String accessToken;
    private String refreshToken;
}