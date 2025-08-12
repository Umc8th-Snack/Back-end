package umc.snack.domain.nlp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class UserInteractionDto {
    private Long articleId;
    private String action;
}
