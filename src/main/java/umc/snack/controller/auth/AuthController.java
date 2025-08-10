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
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;

import umc.snack.common.dto.ApiResponse;
import umc.snack.domain.auth.dto.LoginRequestDto;
import umc.snack.domain.auth.dto.LoginResponseDto;
import umc.snack.domain.auth.dto.SocialLoginResponseDto;
import umc.snack.domain.auth.dto.TokenReissueResponseDto;
import umc.snack.service.auth.GoogleOAuthService;
import umc.snack.service.auth.LogoutService;
import umc.snack.service.auth.ReissueService;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "인증/인가 관련 API")
public class AuthController {

    private final JWTUtil jwtUtil;
    private final ReissueService reissueService;
    private final LogoutService logoutService;
    private final GoogleOAuthService googleOAuthService;

    public AuthController(JWTUtil jwtUtil, ReissueService reissueService, LogoutService logoutService, GoogleOAuthService googleOAuthService) {
        this.jwtUtil = jwtUtil;
        this.reissueService = reissueService;
        this.logoutService = logoutService;
        this.googleOAuthService = googleOAuthService;
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

    @Operation(summary = "구글 소셜 로그인 콜백", description = "구글 인증 서버로부터 받은 인가 코드로 소셜 로그인을 처리합니다.")
    @GetMapping("/google/callback")
    public ResponseEntity<ApiResponse<SocialLoginResponseDto>> googleCallback(@RequestParam(required = false) String code) {
        try {
            // 인가 코드 누락 체크
            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.onFailure("AUTH_2121", "인증 정보가 누락되었습니다.", null));
            }

            // 구글 OAuth 처리
            SocialLoginResponseDto response = googleOAuthService.processGoogleCallback(code);
            
            return ResponseEntity.ok(
                    ApiResponse.onSuccess("AUTH_2020", "소셜 로그인에 성공하였습니다.", response)
            );
            
        } catch (CustomException e) {
            // 서비스에서 발생한 커스텀 예외 처리
            ErrorCode errorCode = e.getErrorCode();
            if (errorCode == ErrorCode.AUTH_2112 || errorCode == ErrorCode.AUTH_2113 || errorCode == ErrorCode.AUTH_2114) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.onFailure("AUTH_2122", "유효하지 않은 토큰입니다.", null));
            } else {
                return ResponseEntity.status(errorCode.getStatus())
                        .body(ApiResponse.onFailure(errorCode.name(), errorCode.getMessage(), null));
            }
        } catch (Exception e) {
            // 예상치 못한 오류
            return ResponseEntity.status(401)
                    .body(ApiResponse.onFailure("AUTH_2122", "유효하지 않은 토큰입니다.", null));
        }
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
