package gyun.sample.domain.log.service;

import gyun.sample.domain.log.entity.PostLog;
import gyun.sample.domain.log.event.PostActivityEvent;
import gyun.sample.domain.log.repository.PostLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostLogEventListener {

    private final PostLogRepository postLogRepository;

    /**
     * 게시글 활동 이벤트 리스너
     * 비동기로 실행되며, 메인 로직의 트랜잭션과 분리되어 로그를 저장합니다.
     */
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePostActivityEvent(PostActivityEvent event) {
        try {
            PostLog logEntity = new PostLog(
                    event.postId(),
                    event.postTitle(),
                    event.executorId(),
                    event.logType(),
                    event.details(),
                    event.clientIp()
            );
            postLogRepository.save(logEntity);

            log.info("[Post Log] ID: {}, Title: {}, Executor: {}, Action: {}",
                    event.postId(), event.postTitle(), event.executorId(), event.logType());
        } catch (Exception e) {
            log.error("게시글 로그 저장 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}