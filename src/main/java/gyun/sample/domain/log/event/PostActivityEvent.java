package gyun.sample.domain.log.event;

import gyun.sample.domain.log.enums.LogType;

public record PostActivityEvent(
        Long postId,
        String postTitle,
        String executorId,
        LogType logType,
        String details,
        String clientIp
) {
    public static PostActivityEvent of(Long postId, String postTitle, String executorId, LogType logType, String details, String clientIp) {
        return new PostActivityEvent(postId, postTitle, executorId, logType, details, clientIp);
    }
}