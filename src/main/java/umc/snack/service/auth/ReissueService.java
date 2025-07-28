package umc.snack.service.auth;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import umc.snack.common.config.security.jwt.JWTUtil;
import umc.snack.common.exception.ErrorCode;
import umc.snack.common.response.ApiResponse;
import umc.snack.domain.auth.dto.TokenReissueResponseDto;
import umc.snack.domain.auth.entity.RefreshToken;
import umc.snack.repository.auth.RefreshTokenRepository;
import umc.snack.repository.user.UserRepository;

@Service
@RequiredArgsConstructor
public class ReissueService {

    private final JWTUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public ResponseEntity<ApiResponse<TokenReissueResponseDto>> reissue(HttpServletRequest request, HttpServletResponse response) {
        // 1. 쿠키에서 refresh token 추출
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }
        if (refreshToken == null) {
            // 인증 정보 누락
            return ResponseEntity
                    .status(ErrorCode.AUTH_2121.getStatus())
                    .body(ApiResponse.fail(
                            "AUTH_2121",
                            ErrorCode.AUTH_2121.getMessage(),
                            null
                    ));
        }

        // 2. DB에 refresh token 존재 확인 (화이트리스트 방식)
        var foundOpt = refreshTokenRepository.findByRefreshToken(refreshToken);
        if (foundOpt.isEmpty()) {
            // 유효하지 않은 토큰
            return ResponseEntity
                    .status(ErrorCode.AUTH_2122.getStatus())
                    .body(ApiResponse.fail(
                            "AUTH_2122",
                            ErrorCode.AUTH_2122.getMessage(),
                            null
                    ));
        }
        RefreshToken found = foundOpt.get();

        // 3. 만료 여부 체크
        try {
            jwtUtil.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {
            // 토큰 만료
            return ResponseEntity
                    .status(ErrorCode.AUTH_2103.getStatus())
                    .body(ApiResponse.fail(
                            "AUTH_2103",
                            ErrorCode.AUTH_2103.getMessage(),
                            null
                    ));
        }

        // 4. refresh 토큰 여부 체크
        String category = jwtUtil.getCategory(refreshToken);
        if (!"refresh".equals(category)) {
            // 유효하지 않은 토큰
            return ResponseEntity
                    .status(ErrorCode.AUTH_2162.getStatus())
                    .body(ApiResponse.fail(
                            "AUTH_2162",
                            ErrorCode.AUTH_2162.getMessage(),
                            null
                    ));
        }

        // 5. user 정보 파싱 및 조회
        Long userId = jwtUtil.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElse(null);
        if (user == null) {
            // 등록되지 않은 이메일 (유저)
            return ResponseEntity
                    .status(ErrorCode.AUTH_2141.getStatus())
                    .body(ApiResponse.fail(
                            "AUTH_2141",
                            ErrorCode.AUTH_2141.getMessage(),
                            null
                    ));
        }

        // 6. 토큰 재발급 (access + refresh)
        String role = jwtUtil.getRole(refreshToken);
        String newAccess = jwtUtil.createJwt("access", userId, role, 600_000L);         // 10분
        String newRefresh = jwtUtil.createJwt("refresh", userId, role, 86_400_000L);    // 1일

        // 기존 refreshToken 폐기, 새로운 refreshToken 저장 (화이트리스트 정책)
        refreshTokenRepository.delete(found);
        refreshTokenRepository.save(
                RefreshToken.builder()
                        .userId(userId)
                        .email(user.getEmail())
                        .refreshToken(newRefresh)
                        .expiration(found.getExpiration().plusDays(1)) // 또는 LocalDateTime.now().plusDays(1)
                        .build()
        );

        // access 토큰은 header, refresh 토큰은 쿠키
        response.setHeader("access", newAccess);
        response.addCookie(createCookie("refresh", newRefresh));

        // 7. 응답 Dto 만들기
        TokenReissueResponseDto dto = TokenReissueResponseDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .build();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "AUTH_2060",
                        "Access 토큰이 재발급되었습니다.",
                        dto
                )
        );
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60);
        cookie.setHttpOnly(true);
        // cookie.setPath("/");
        return cookie;
    }
}