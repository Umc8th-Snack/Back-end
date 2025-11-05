package umc.snack.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // 1. Spring의 비동기 기능을 활성화합니다.
public class AsyncConfig {

    // 2. NlpService에서 @Async("taskExecutor")로 호출한 바로 그 스레드 풀입니다.
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);   // 1. 기본 실행 대기 스레드 수
        executor.setMaxPoolSize(10);  // 2. 최대 스레드 수
        executor.setQueueCapacity(100); // 3. 큐 용량 (작업 대기열)
        executor.setThreadNamePrefix("Async-task-"); // 스레드 이름 접두사
        executor.initialize();

        return executor;
    }
}