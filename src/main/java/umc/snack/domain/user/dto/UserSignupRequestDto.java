package umc.snack.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSignupRequestDto {

//    @Email(message = "이메일 형식이 올바르지 않습니다.") // GlobalExceptionHandler 코드 변경하면 주석 풀어도 됨.
    @NotBlank
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(min = 2, max = 6, message = "닉네임은 2자 이상 6자 이하이어야 합니다.")
    private String nickname;

}
