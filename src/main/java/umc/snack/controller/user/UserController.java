package umc.snack.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import umc.snack.common.config.security.CustomUserDetails;
import umc.snack.common.dto.ApiResponse;
import umc.snack.domain.user.dto.*;
import umc.snack.domain.user.entity.User;
import umc.snack.service.user.UserService;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "회원 관련 API")
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입",description = "이메일, 비밀번호, 닉네임으로 회원가입합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserSignupResponseDto>> signup(@RequestBody @Valid UserSignupRequestDto request) {

        UserSignupResponseDto result = userService.signup(request);
        return ResponseEntity.ok(
                ApiResponse.onSuccess("USER_2500", "회원가입이 정상적으로 처리되었습니다.", result)
        );
    }

    @Operation(summary = "내 비밀번호 변경", description = "현재 비밀번호와 새 비밀번호를 입력하여 비밀번호를 변경합니다.")
    @PatchMapping("/me/password")
    public ResponseEntity<?> updatePassword(@RequestBody Object request) {
        // TODO: 구현 예정
        return ResponseEntity.ok("구현 예정");
    }

    @Operation(summary = "마이페이지 정보 조회", description = "내 회원 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponseDto>> getMyPage() {
        UserInfoResponseDto result = userService.getCurrentUserInfo();
        return ResponseEntity.ok(
                ApiResponse.onSuccess("USER_2530", "사용자 정보 조회에 성공했습니다", result)
        );
    }

    @Operation(summary = "회원탈퇴", description = "내 계정(회원)을 탈퇴합니다.")
    @PostMapping("/me/withdraw")
    public ResponseEntity<?> withdrawUser(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody UserWithdrawRequestDto request) {
        userService.withdraw(userDetails.getUser(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.onSuccess("USER_2520","회원 탈퇴가 정상적으로 처리되었습니다.",null));
    }

    @Operation(summary = "내 정보 수정", description = "닉네임, 프로필 이미지, 소개 등 내 정보를 수정합니다.")
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserUpdateResponseDto>> updateMyInfo(@RequestBody @Valid UserUpdateRequestDto request) {
        UserUpdateResponseDto result = userService.updateMyInfo(request);
        return ResponseEntity.ok(
                ApiResponse.onSuccess("USER_2540", "사용자 정보가 성공적으로 수정되었습니다", result)
        );
    }
}