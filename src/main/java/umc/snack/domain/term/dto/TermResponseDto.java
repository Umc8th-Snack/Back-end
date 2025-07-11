package umc.snack.domain.term.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TermResponseDto {
    private String word;
    private List<String> definitions; // API 명세에 맞춰 List<String>으로 정의
    private LocalDateTime createdAt;
}
