package umc.snack.common.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties("spring.jwt.token")
public class JwtProperties {
    private String secretKey="";
    private Expiration expiration;

    @Getter
    @Setter
    public static class Expiration{
        private Long access;
        private Long refresh;    }
}
