package umc.snack.common.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .additionalInterceptors((request, body, execution) -> {
                    return execution.execute(request, body);
                })
                .errorHandler(new DefaultResponseErrorHandler())
                .build();
    }

    @Bean
    @Qualifier("fastApiRestTemplate")
    public RestTemplate fastApiRestTemplate(
            @Value("${fastapi.url}") String baseUrl,
            RestTemplateBuilder builder) {
        return builder
                .rootUri(baseUrl)
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}