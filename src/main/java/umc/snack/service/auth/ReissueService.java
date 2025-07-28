package umc.snack.service.auth;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import umc.snack.common.config.security.jwt.JWTUtil;
import umc.snack.common.exception.ErrorCode;
import umc.snack.common.response.ApiResponse;
import umc.snack.domain.auth.entity.RefreshToken;
import umc.snack.repository.auth.RefreshTokenRepository;

@Service
public class ReissueService {

    private final JWTUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    public ReissueService(JWTUtil jwtUtil, RefreshTokenRepository refreshTokenRepository) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public ResponseEntity<ApiResponse<Void>> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refresh")) {
                    refreshToken = cookie.getValue();
                }
            }
        }

        if (refreshToken == null) {
            return ResponseEntity
                    .status(ErrorCode.AUTH_2101.getStatus())
                    .body(ApiResponse.fail(
                            "AUTH_2101",
                            ErrorCode.AUTH_2101.getMessage(),
                            null
                    ));
        }

        if (refreshTokenRepository.findByRefreshToken(refreshToken).isEmpty()) {
            return ResponseEntity
                    .status(ErrorCode.AUTH_2102.getStatus())
                    .body(ApiResponse.fail(
                            "AUTH_2102",
                            ErrorCode.AUTH_2102.getMessage(),
                            null
                    ));
        }

        try {
            jwtUtil.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {
            return ResponseEntity
                    .status(ErrorCode.AUTH_2103.getStatus())
                    .body(ApiResponse.fail(
                            "AUTH_2103",
                            ErrorCode.AUTH_2103.getMessage(),
                            null
                    ));
        }

        String category = jwtUtil.getCategory(refreshToken);
        if (!category.equals("refresh")) {
            return ResponseEntity
                    .status(ErrorCode.AUTH_2102.getStatus())
                    .body(ApiResponse.fail(
                            "AUTH_2102",
                            "유효하지 않은 refresh token입니다.",
                            null
                    ));
        }

        String role = jwtUtil.getRole(refreshToken);
        Long userId = jwtUtil.getUserId(refreshToken);

        String newAccess = jwtUtil.createJwt("access", userId, role, 600_000L);
        String newRefresh = jwtUtil.createJwt("refresh", userId, role, 86_400_000L);

        response.setHeader("access", newAccess);
        response.addCookie(createCookie("refresh", newRefresh));

        return ResponseEntity.ok(ApiResponse.success(
                null, "토큰 재발급 성공", null
        ));
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60);
        cookie.setHttpOnly(true);
        return cookie;
    }
}