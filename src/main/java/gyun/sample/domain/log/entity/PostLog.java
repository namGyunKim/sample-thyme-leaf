package gyun.sample.domain.log.entity;

import gyun.sample.domain.account.entity.BaseTimeEntity;
import gyun.sample.domain.log.enums.LogType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post_log", indexes = {
        @Index(name = "idx_post_log_post_id", columnList = "postId"),
        @Index(name = "idx_post_log_created_at", columnList = "createdAt")
})
public class PostLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @Comment("대상 게시글 ID")
    private Long postId;

    @Comment("게시글 제목 (로그 시점의 제목 스냅샷)")
    private String postTitle;

    @Comment("작업 수행자 ID (회원 ID or 'ANONYMOUS')")
    private String executorId;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(255)")
    @Comment("활동 유형")
    private LogType logType;

    @Comment("상세 내용")
    private String details;

    @Comment("요청 IP")
    private String clientIp;

    public PostLog(Long postId, String postTitle, String executorId, LogType logType, String details, String clientIp) {
        this.postId = postId;
        this.postTitle = postTitle;
        this.executorId = executorId;
        this.logType = logType;
        this.details = details;
        this.clientIp = clientIp;
    }
}