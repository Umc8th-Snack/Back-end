package umc.snack.common.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    @Qualifier("fastApiRestTemplate")
    public RestTemplate fastApiRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000); // 연결시간 5000ms = 5s
        requestFactory.setReadTimeout(10000); // 읽는 시간 10초

        return new RestTemplate(requestFactory);
    }

    @Bean
    @Qualifier("longTimeoutRestTemplate")
    public RestTemplate longTimeoutRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000); // 연결시간 10초
        requestFactory.setReadTimeout(300000); // 읽는 시간 5분...
        return new RestTemplate(requestFactory);
    }
}