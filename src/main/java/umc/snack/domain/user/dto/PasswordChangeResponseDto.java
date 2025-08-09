package umc.snack.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PasswordChangeResponseDto {


    private Long userId;
    private LocalDateTime changedAt;

    @Builder
    public PasswordChangeResponseDto(Long userId, LocalDateTime changedAt) {
        this.userId = userId;
        this.changedAt = changedAt;
    }
}
