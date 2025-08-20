package umc.snack.common.config.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

public class CookieUtil {
    // Cross-Domain 환경을 지원하는 쿠키 생성 메서드 (ResponseCookie 사용)
    public static void createCookie(String key, String value, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(key, value)
                .maxAge(24 * 60 * 60)  // 1일
                .httpOnly(true)
                .secure(true)  // HTTPS 환경에서만 전송
                .path("/")
                .sameSite("None")  // Cross-Domain 허용
                .build();
        
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
    
    // 쿠키 삭제를 위한 메서드
    public static void deleteCookie(String key, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(key, "")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("None")
                .build();
        
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
