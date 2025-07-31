package umc.snack.common.config.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 엔드포인트
                .allowedOriginPatterns("https://snack-front-end.vercel.app") // 모든 origin 허용 (와일드카드)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // 주요 메서드
                .allowedHeaders("*") // 헤더 제한 없음
                .allowCredentials(true); // 쿠키, 헤더 인증 허용
    }
}
