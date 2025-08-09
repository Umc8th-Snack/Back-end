package umc.snack.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import umc.snack.domain.user.entity.User;

@Getter
@Schema(description = "사용자 정보 수정 응답 DTO")
public class UserUpdateResponseDto {
    
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    
    @Schema(description = "닉네임", example = "새로운닉네임")
    private String nickname;
    
    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profiles/new_image.jpg")
    private String profileImage;

    @Builder
    public UserUpdateResponseDto(Long userId, String nickname, String profileImage) {
        this.userId = userId;
        this.nickname = nickname;
        this.profileImage = profileImage;
    }

    public static UserUpdateResponseDto fromEntity(User user) {
        return UserUpdateResponseDto.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .build();
    }
}