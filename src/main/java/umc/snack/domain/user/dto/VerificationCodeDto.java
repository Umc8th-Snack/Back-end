package umc.snack.domain.user.dto;

import lombok.Getter;

@Getter
public class VerificationCodeDto {

    private String email;

    private String code;

}
