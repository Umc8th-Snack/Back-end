package umc.snack.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import umc.snack.common.response.ApiResponse;
import umc.snack.domain.user.dto.UserSignupRequestDto;
import umc.snack.domain.user.dto.UserSignupResponseDto;
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
                ApiResponse.success("USER_2501", "회원가입이 정상적으로 처리되었습니다.", result)
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
    public ResponseEntity<?> getMyPage() {
        // TODO: 구현 예정
        return ResponseEntity.ok("구현 예정");
    }

    @Operation(summary = "회원탈퇴", description = "내 계정(회원)을 탈퇴합니다.")
    @DeleteMapping("/me")
    public ResponseEntity<?> withdrawUser() {
        // TODO: 구현 예정
        return ResponseEntity.ok("구현 예정");
    }

    @Operation(summary = "내 정보 수정", description = "닉네임, 소개 등 내 정보를 수정합니다.")
    @PatchMapping("/me")
    public ResponseEntity<?> updateMyInfo(@RequestBody Object request) {
        // TODO: 구현 예정
        return ResponseEntity.ok("구현 예정");
    }
}