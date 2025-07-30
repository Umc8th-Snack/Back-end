package umc.snack.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter @Setter
public class TokenReissueResponseDto {

    private Long userId;
    private String email;
    private String nickname;
    private String accessToken;
    private String refreshToken;

    public String getNickname() { return nickname; }

}
