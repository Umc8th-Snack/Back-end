package umc.snack.domain.nlp.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleVectorizeListRequestDto {
    private List<ArticleVectorizeRequestDto> articles;
}