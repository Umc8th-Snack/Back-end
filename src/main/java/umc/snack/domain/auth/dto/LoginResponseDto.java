package umc.snack.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {
    private Long userId;
    private String email;
    private String nickname;
    private String accessToken;
    private String refreshToken;

}