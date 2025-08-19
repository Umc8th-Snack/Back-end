package umc.snack.domain.user.dto;

import lombok.Getter;

@Getter
public class PasswordResetDto {

    private String email;

    private String newPassword;

    private String confirmPassword;
}