package gyun.sample.domain.log.service;

import gyun.sample.domain.log.entity.MemberLog;
import gyun.sample.domain.log.event.MemberActivityEvent;
import gyun.sample.domain.log.repository.MemberLogRepository;
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
public class MemberLogEventListener {

    private final MemberLogRepository memberLogRepository;

    /**
     * 회원 활동 이벤트 리스너
     * 메인 트랜잭션과 분리하여 저장하거나, 비동기로 처리하여 성능 영향을 최소화합니다.
     */
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleMemberActivityEvent(MemberActivityEvent event) {
        try {
            // executorId 파라미터 제거
            MemberLog logEntity = new MemberLog(
                    event.loginId(),
                    event.memberId(),
                    event.logType(),
                    event.details(),
                    event.clientIp()
            );

            // save 호출 시 JpaAuditing에 의해 createdBy(수행자)가 자동으로 채워짐
            // (AsyncConfig의 TaskDecorator 덕분에 비동기 스레드에서도 SecurityContext 접근 가능)
            memberLogRepository.save(logEntity);

            // 로그 출력 시에는 createdBy를 아직 알 수 없으므로(save 전/후 flush 필요)
            // SecurityContext에서 직접 꺼내거나, 단순히 "Recorded" 등으로 남김
            log.info("[Activity Log] Target: {}, Action: {}", event.loginId(), event.logType());
        } catch (Exception e) {
            log.error("로그 저장 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}