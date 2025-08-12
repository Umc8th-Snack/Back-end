package umc.snack.common.config.security;

import jakarta.servlet.http.Cookie;

public class CookieUtil {
    // 쿠키 생성 메서드
    public static Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
//        cookie.setMaxAge(24 * 60 * 60);
        cookie.setMaxAge(10 * 60);
        cookie.setHttpOnly(true);
//        cookie.setSecure(true);  // 배포 환경에서만 활성화
        cookie.setPath("/");
        return cookie;
    }
}
