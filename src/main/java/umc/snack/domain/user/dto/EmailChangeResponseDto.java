package umc.snack.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmailChangeResponseDto {

    private String email;
    private String updatedAt;

}
