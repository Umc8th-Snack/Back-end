package umc.snack.domain.feed.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class CategoryDto {
    private Long categoryId;
    private String categoryName;
}