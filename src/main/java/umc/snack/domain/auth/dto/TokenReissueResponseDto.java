package umc.snack.domain.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TokenReissueResponseDto {

    private Long userId;
    private String email;
    private String nickname;
    private String accessToken;
    private String refreshToken;
}
