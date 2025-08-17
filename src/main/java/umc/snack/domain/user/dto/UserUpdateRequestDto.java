package umc.snack.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "사용자 정보 수정 요청 DTO")
public class UserUpdateRequestDto {
    
    @Schema(description = "닉네임", example = "새로운닉네임")
    private String nickname;
    
    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profiles/new_image.jpg")
    private String profileImage;
    
    @Schema(description = "소개", example = "안녕하세요, 새로운 소개입니다.")
    private String introduction;
}