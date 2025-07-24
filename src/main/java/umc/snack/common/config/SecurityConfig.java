package umc.snack.common.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authz) -> authz
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/swagger-ui.html",
                                "/webjars/**",
                                // 전체 공개 api
                                "/api/users/signup",
                                "/api/auth/login",
                                "/api/auth/kakao",
                                "/auth/kakao/callback",
                                "/api/articles/*/related-articles",
                                "/api/articles/search",
                                "/api/articles/main",
                                // 관리자 공개 api -> 개발 단계에서는 전체 공개
                                "/api/articles/crawl/status",
                                "/api/articles/*/summarize",
                                "/api/terms",

                                //스크랩 테스트용
                                "/api/scraps/**",
                                "/api/articles/**"
                        ).permitAll()
                        // 나머지는 모두 JWT 토큰 인증 필요
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
