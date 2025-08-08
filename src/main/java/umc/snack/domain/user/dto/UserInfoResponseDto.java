package umc.snack.domain.user.dto;

import lombok.Builder;
import lombok.Getter;
import umc.snack.domain.user.entity.User;

@Getter
public class UserInfoResponseDto {
    private Long userId;
    private String email;
    private String nickname;
    private String profileImage;
    private String introduction;

    @Builder
    public UserInfoResponseDto(Long userId, String email, String nickname, String profileImage, String introduction) {
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.introduction = introduction;
    }

    public static UserInfoResponseDto fromEntity(User user) {
        return UserInfoResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .introduction(user.getIntroduction())
                .build();
    }
}