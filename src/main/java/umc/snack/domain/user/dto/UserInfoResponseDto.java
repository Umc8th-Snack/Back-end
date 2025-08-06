package umc.snack.domain.user.dto;

import lombok.Builder;
import lombok.Getter;
import umc.snack.domain.user.entity.User;

@Getter
public class UserInfoResponseDto {
    private Long userId;
    private String email;
    private String nickname;
    private String profileUrl;
    private String introduction;

    @Builder
    public UserInfoResponseDto(Long userId, String email, String nickname, String profileUrl, String introduction) {
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.profileUrl = profileUrl;
        this.introduction = introduction;
    }

    public static UserInfoResponseDto fromEntity(User user) {
        return UserInfoResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileUrl(user.getProfileUrl())
                .introduction(user.getIntroduction())
                .build();
    }
}