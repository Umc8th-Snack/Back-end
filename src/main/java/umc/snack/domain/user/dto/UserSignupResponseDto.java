package umc.snack.domain.user.dto;

import lombok.Builder;
import lombok.Getter;
import umc.snack.domain.user.entity.User;

import java.time.LocalDateTime;
@Getter
public class UserSignupResponseDto {
    private Long userId;
    private String email;
    private String nickname;
    private String createdAt;

    @Builder
    public UserSignupResponseDto(Long userId, String email, String nickname, String createdAt) {
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.createdAt = createdAt;
    }

    public static UserSignupResponseDto fromEntity(User user) {
        return UserSignupResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .createdAt(user.getCreatedAt().toString())
                .build();
    }

}
