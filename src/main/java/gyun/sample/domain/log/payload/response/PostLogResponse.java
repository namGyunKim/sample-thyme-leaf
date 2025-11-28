package gyun.sample.domain.log.payload.response;

import gyun.sample.domain.log.entity.PostLog;
import gyun.sample.domain.log.enums.LogType;
import gyun.sample.global.utils.UtilService;

public record PostLogResponse(
        Long id,
        Long postId,        // 게시글 ID
        String postTitle,   // 게시글 제목 (로그 시점)
        String executorId,  // 수행자 ID
        LogType logType,    // 로그 유형
        String details,     // 상세 내용
        String clientIp,    // IP 주소
        String createdAt    // 생성일시
) {
    public PostLogResponse(PostLog log) {
        this(
                log.getId(),
                log.getPostId(),
                log.getPostTitle(),
                log.getExecutorId(),
                log.getLogType(),
                log.getDetails(),
                log.getClientIp(),
                UtilService.formattedTime(log.getCreatedAt())
        );
    }
}