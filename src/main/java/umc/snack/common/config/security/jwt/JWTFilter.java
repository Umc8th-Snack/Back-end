package umc.snack.common.config.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import umc.snack.common.config.security.CustomUserDetails;
import umc.snack.common.exception.ErrorCode;
import umc.snack.common.dto.ApiResponse;
import umc.snack.domain.user.entity.User;
import umc.snack.repository.auth.RefreshTokenRepository;
import umc.snack.repository.user.UserRepository;

import java.io.IOException;
import java.io.PrintWriter;

public class JWTFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);

    private final JWTUtil jwtUtil;
    private UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public JWTFilter(JWTUtil jwtUtil, UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {

        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 재발급 요청은 Access 토큰 검증 스킵
        String path = request.getRequestURI();
        if ("/api/auth/reissue".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 헤더에서 access키에 담긴 토큰을 꺼냄
        String authorizationHeader = request.getHeader("Authorization");
        String accessToken = null;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            accessToken = authorizationHeader.substring(7); // "Bearer " 다음부터가 실제 토큰
        }
        // 토큰이 없다면 다음 필터로 넘김
        if (accessToken == null) {

            filterChain.doFilter(request, response);

            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();


        // 토큰 만료 여부 확인
        try {
            if (jwtUtil.isExpired(accessToken)) {
                setErrorResponse(response, objectMapper, ErrorCode.AUTH_2166); // Access 토큰 만료
                return;
            }
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            setErrorResponse(response, objectMapper, ErrorCode.AUTH_2166); // Access 토큰 만료
            return;
        } catch (Exception e) {
            setErrorResponse(response, objectMapper, ErrorCode.AUTH_2161); // 유효하지 않은 Access 토큰
            return;
        }

        // category 체크 (access인지)
        String category;
        try {
            category = jwtUtil.getCategory(accessToken);
        } catch (Exception e) {
            setErrorResponse(response, objectMapper, ErrorCode.AUTH_2161); // 유효하지 않은 Access 토큰
            return;
        }
        if (!"access".equals(category)) {
            setErrorResponse(response, objectMapper, ErrorCode.AUTH_2162); // 유효하지 않은 Refresh 토큰
            return;
        }

        // userId, role 추출 및 User 조회
        Long userId;
        String role;
        try {
            userId = jwtUtil.getUserId(accessToken);
            role = jwtUtil.getRole(accessToken);
        } catch (Exception e) {
            setErrorResponse(response, objectMapper, ErrorCode.AUTH_2161); // 유효하지 않은 Access 토큰
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            setErrorResponse(response, objectMapper, ErrorCode.AUTH_2101); // 등록되지 않은 이메일입니다.
            return;
        }
        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        //SecurityContextHolder 설정 추가(채원)
        Authentication authentication = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private void setErrorResponse(HttpServletResponse response, ObjectMapper objectMapper, ErrorCode errorCode) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(errorCode.getStatus());
        ApiResponse<?> apiResponse = ApiResponse.onFailure(
                errorCode.name(),
                errorCode.getMessage(),
                null
        );
        String json = objectMapper.writeValueAsString(apiResponse);
        response.getWriter().print(json);
        response.getWriter().flush();
    }


}
