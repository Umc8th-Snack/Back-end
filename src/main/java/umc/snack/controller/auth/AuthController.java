package umc.snack.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import umc.snack.common.config.security.jwt.JWTUtil;

import umc.snack.common.dto.ApiResponse;
import umc.snack.domain.auth.dto.LoginRequestDto;
import umc.snack.domain.auth.dto.LoginResponseDto;
import umc.snack.domain.auth.dto.TokenReissueResponseDto;
import umc.snack.service.auth.LogoutService;
import umc.snack.service.auth.ReissueService;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "인증/인가 관련 API")
public class AuthController {

    private final JWTUtil jwtUtil;
    private final ReissueService reissueService;
    private final LogoutService logoutService;

    public AuthController(JWTUtil jwtUtil, ReissueService reissueService, LogoutService logoutService) {
        this.jwtUtil = jwtUtil;
        this.reissueService = reissueService;
        this.logoutService = logoutService;
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@RequestBody LoginRequestDto request) {
        // 실제로는 LoginFilter가 가로챔
        return ResponseEntity.ok(ApiResponse.onSuccess("AUTH_2000", "로그인에 성공하였습니다.", null));
    }

    @Operation(summary = "Access Token, Refresh Token 재발급", description = "만료된 access token(혹은 토큰이 없는 상태)에서 refresh token을 이용해 새로운 access token과 refresh token을 모두 재발급합니다. " +
            "기존 refresh token은 폐기되고, 새 refresh token만 사용됩니다. access token은 response header, refresh token은 쿠키로 전달됩니다.")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenReissueResponseDto>> reissue(HttpServletRequest request, HttpServletResponse response) {
        return reissueService.reissue(request, response);
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

    @Operation(summary = "로그아웃", description = "회원의 refresh 토큰을 만료시켜 로그아웃합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(HttpServletRequest request, HttpServletResponse response) {
        return logoutService.logout(request, response);
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
