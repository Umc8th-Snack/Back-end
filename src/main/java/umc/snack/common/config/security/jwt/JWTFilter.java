package umc.snack.common.config.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import umc.snack.common.config.security.CustomUserDetails;
import umc.snack.domain.user.entity.User;
import umc.snack.repository.user.UserRepository;

import java.io.IOException;

public class JWTFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);

    private final JWTUtil jwtUtil;
    private UserRepository userRepository;

    public JWTFilter(JWTUtil jwtUtil, UserRepository userRepository) {

        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        // Authorization 헤더 검증
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            logger.debug("Authorization header is missing or invalid");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.split(" ")[1];

        String email = null;
        String role = null;

        try {
            // 토큰 만료 검증
            if (jwtUtil.isExpired(token)) {
                logger.debug("JWT token is expired");
                filterChain.doFilter(request, response);
                return;
            }

            // 토큰에서 username과 role 획득
            email = jwtUtil.getEmail(token);
            role = jwtUtil.getRole(token);

        } catch (Exception e) {
            // 여기서 JWT 파싱 관련 모든 예외 처리 (만료/변조/서명오류 등)
            System.out.println("invalid token: " + e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        User user = userRepository.findByEmail(email);

        if (user == null) {
            logger.debug("User not found for email: {}", email);
            filterChain.doFilter(request, response);
            return;
        }

        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        Authentication authToken = new UsernamePasswordAuthenticationToken(
                customUserDetails, null, customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }

}
