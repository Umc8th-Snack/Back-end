package umc.snack.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmailChangeRequestDto {

    private String newEmail;
    private String currentPassword;

    public EmailChangeRequestDto(String newEmail, String currentPassword) {
        this.newEmail = newEmail;
        this.currentPassword = currentPassword;
    }
}
