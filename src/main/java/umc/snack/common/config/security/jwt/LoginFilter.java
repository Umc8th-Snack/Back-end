package umc.snack.common.config.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import umc.snack.common.config.security.CustomUserDetails;
import umc.snack.common.dto.ApiResponse;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.auth.dto.LoginResponseDto;
import umc.snack.domain.auth.entity.RefreshToken;
import umc.snack.repository.auth.RefreshTokenRepository;
import umc.snack.service.auth.ReissueService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JWTUtil jwtUtil;
    private final ReissueService reissueService;

    @Value("${spring.jwt.token.expiration.access}")
    private Long accessExpiredMs;

    @Value("${spring.jwt.token.expiration.refresh}")
    private Long refreshExpiredMs;

    public LoginFilter(AuthenticationManager authenticationManager,
                       JWTUtil jwtUtil,
                       ReissueService reissueService,
                       RefreshTokenRepository refreshTokenRepository,
                       Long accessExpiredMs,
                       Long refreshExpiredMs) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
        this.reissueService = reissueService;
        this.accessExpiredMs = accessExpiredMs;
        this.refreshExpiredMs = refreshExpiredMs;
        setUsernameParameter("email");
        setFilterProcessesUrl("/api/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // JSON 바디 파싱
            var requestMap = objectMapper.readValue(request.getInputStream(), Map.class);
            String email = (String) requestMap.get("email");
            String password = (String) requestMap.get("password");

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password);
            return authenticationManager.authenticate(authToken);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = customUserDetails.getUserId(); // userId 추출
        String email = customUserDetails.getUsername();
        String nickname = customUserDetails.getUser().getNickname();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String role = authorities.iterator().next().getAuthority();

        // 토큰 생성
        String accessToken = jwtUtil.createJwt("access", userId, email, role, accessExpiredMs); // 30분
        String refreshToken = jwtUtil.createJwt("refresh", userId, email, role, refreshExpiredMs); // 1일

        // refresh 토큰 저장
        LocalDateTime expirationDate = LocalDateTime.now().plusSeconds(refreshExpiredMs / 1000); // 1일
        reissueService.replaceRefreshToken(userId, email, refreshToken, expirationDate);

        // 응답 DTO
        LoginResponseDto resultDto = LoginResponseDto.builder()
                .userId(userId)
                .email(email)
                .nickname(nickname)
                .build();

        ApiResponse<LoginResponseDto> apiResponse = ApiResponse.onSuccess(
                "AUTH_2000",
                "로그인에 성공하였습니다.",
                resultDto
        );

        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(HttpStatus.OK.value());
        try {
            ObjectMapper mapper = new ObjectMapper();
            response.getWriter().write(mapper.writeValueAsString(apiResponse));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 헤더/쿠키 유지
        response.setHeader("Authorization", "Bearer " + accessToken);
        response.addCookie(createCookie("refresh", refreshToken));
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {
        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String code;
        String message;

        // 예외 종류별로 코드/메시지 세팅 (실제 메시지는 예외 구현체에 따라 다를 수 있음)
        if (failed instanceof UsernameNotFoundException) {
            code = ErrorCode.AUTH_2101.name();
            message =  ErrorCode.AUTH_2101.getMessage();
        } else if (failed instanceof BadCredentialsException) {
            code = ErrorCode.AUTH_2102.name();
            message = ErrorCode.AUTH_2102.getMessage();
        } else {
            code = ErrorCode.AUTH_2103.name();
            message = ErrorCode.AUTH_2103.getMessage();
        }

        // ApiResponse 생성 (에러 상세 정보 필요 없으면 result만 null)
        ApiResponse<Object> apiResponse = ApiResponse.onFailure(code, message, null);

        try {
            String json = new ObjectMapper().writeValueAsString(apiResponse);
            response.getWriter().write(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 쿠키 생성 메서드
    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60);
        cookie.setHttpOnly(true);
//        cookie.setSecure(true);
        cookie.setPath("/");
        return cookie;
    }

}
