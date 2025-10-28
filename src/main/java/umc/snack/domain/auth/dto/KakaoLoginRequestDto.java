package umc.snack.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class KakaoLoginRequestDto {

    @NotBlank(message = "카카오 액세스 토큰이 필요합니다.")
    private String accessToken;
}


