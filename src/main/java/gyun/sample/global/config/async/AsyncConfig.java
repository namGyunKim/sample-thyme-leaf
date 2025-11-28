package gyun.sample.global.config.async;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 공통 비동기 처리를 위한 Executor 설정
     * - Virtual Thread 사용으로 높은 처리량 보장
     * - MDC(TraceId) 및 SecurityContext(인증 정보) 전파 기능 포함
     * - @Primary를 사용하여 별도의 이름 지정 없이 @Async를 사용할 때 기본으로 적용됨
     */
    @Bean(name = {"taskExecutor", "emailTaskExecutor"}) // 기본 이름 및 이메일용 별칭 지정
    @Primary
    public AsyncTaskExecutor taskExecutor() {
        // 가상 스레드 기반의 Executor 생성
        TaskExecutorAdapter adapter = new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());

        // 데코레이터를 통해 메인 스레드의 컨텍스트(로그, 인증)를 비동기 스레드로 복사
        adapter.setTaskDecorator(runnable -> {
            // 1. [메인 스레드] 현재 컨텍스트 캡처
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            SecurityContext securityContext = SecurityContextHolder.getContext();

            return () -> {
                try {
                    // 2. [비동기 스레드] 캡처한 컨텍스트 설정
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    }
                    if (securityContext != null) {
                        SecurityContextHolder.setContext(securityContext);
                    }

                    // 실제 작업 실행
                    runnable.run();
                } finally {
                    // 3. [비동기 스레드] 작업 완료 후 정리 (스레드 풀 오염 방지)
                    MDC.clear();
                    SecurityContextHolder.clearContext();
                }
            };
        });

        return adapter;
    }
}