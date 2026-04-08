package com.example.konnect_backend.global.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public ThreadPoolTaskExecutor promptExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(1);
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setThreadNamePrefix("prompt-");
        executor.initialize();
        return executor;
    }

    private static class MdcTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // 현재(부모) 스레드의 MDC 컨텍스트 복사
            Map<String, String> contextMap = MDC.getCopyOfContextMap();

            return () -> {
                try {
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap); // 자식 스레드에 주입
                    }
                    runnable.run();
                } finally {
                    MDC.clear(); // 작업 완료 후 자식 스레드 MDC 정리
                }
            };
        }
    }
}
