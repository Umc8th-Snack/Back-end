package umc.snack.domain.nlp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter @AllArgsConstructor @NoArgsConstructor
public class UserProfileRequestDto {
    private Long userId;
    private List<UserInteractionDto> interactions;
}
