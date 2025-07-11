package umc.snack.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증/인가 관련 API")
public class AuthController {

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Object request) {
        // TODO: 구현 예정
        return ResponseEntity.ok("구현 예정");
    }

    @Operation(summary = "카카오 소셜 로그인", description = "인가 코드를 전달하여 카카오 소셜 로그인을 시작합니다.")
    @GetMapping("/kakao")
    public ResponseEntity<?> kakaoLogin() {
        // TODO: 구현 예정
        return ResponseEntity.ok("구현 예정");
    }

    @Operation(summary = "카카오 소셜 로그인 콜백 주소", description = "카카오 인가코드로 로그인 처리를 완료합니다.")
    @GetMapping("/kakao/callback")
    public ResponseEntity<?> kakaoCallback(@RequestParam(required = false) String code) {
        // TODO: 구현 예정
        return ResponseEntity.ok("구현 예정");
    }

    @Operation(summary = "로그아웃", description = "회원의 액세스 토큰을 만료시켜 로그아웃합니다.")
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // TODO: 구현 예정
        return ResponseEntity.ok("구현 예정");
    }

    @Operation(summary = "이메일 인증코드 전송", description = "입력한 이메일로 인증코드를 전송합니다.")
    @PostMapping("/email/send-code")
    public ResponseEntity<?> sendEmailCode(@RequestBody Object request) {
        // TODO: 구현 예정
        return ResponseEntity.ok("구현 예정");
    }

    @Operation(summary = "이메일 인증코드 검증", description = "이메일로 받은 인증코드가 올바른지 검증합니다.")
    @PostMapping("/email/verify-code")
    public ResponseEntity<?> verifyEmailCode(@RequestBody Object request) {
        // TODO: 구현 예정
        return ResponseEntity.ok("구현 예정");
    }
}
