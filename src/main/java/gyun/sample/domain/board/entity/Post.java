package gyun.sample.domain.board.entity;

import gyun.sample.domain.account.entity.BaseTimeEntity;
import gyun.sample.domain.board.enums.PostType;
import gyun.sample.domain.board.payload.request.PostUpdateRequest;
import gyun.sample.domain.member.entity.Member;
import gyun.sample.global.enums.GlobalActiveEnums;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// [성능 개선] 검색 조건으로 자주 사용되는 postType과 active에 복합 인덱스 추가
@Table(name = "post", indexes = {
        @Index(name = "idx_post_type_active", columnList = "postType, active"),
        @Index(name = "idx_post_created_at", columnList = "createdAt") // 최신순 정렬 성능 향상
})
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @Comment("게시글 제목")
    private String title;

    @Column(columnDefinition = "TEXT")
    @Comment("게시글 내용")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(255)")
    @Comment("게시판 타입 (FREE, SUGGESTION)")
    private PostType postType;

    @Comment("조회수")
    private int viewCount = 0;

    @Comment("좋아요 수")
    private int likeCount = 0;

    @Comment("상단 고정 여부")
    private boolean isPinned = false;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(255)")
    @Comment("게시글 상태 (ACTIVE, INACTIVE)")
    private GlobalActiveEnums active = GlobalActiveEnums.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member author;

    // 생성자
    public Post(String title, String content, PostType postType, Member author) {
        this.title = title;
        this.content = content;
        this.postType = postType;
        this.author = author;
    }

    // 수정 메소드
    public void update(PostUpdateRequest request) {
        this.title = request.title();
        this.content = request.content();
    }

    // 조회수 증가 (메모리 상에서만 증가, 실제 DB 반영은 Bulk Update 권장)
    public void increaseViewCount() {
        this.viewCount++;
    }

    // 좋아요 증가
    public void increaseLikeCount() {
        this.likeCount++;
    }

    // 좋아요 감소 (0 이하로 내려가지 않도록 보호)
    public void decreaseLikeCount() {
        this.likeCount = Math.max(0, this.likeCount - 1);
    }

    // 삭제 (Soft Delete)
    public void delete() {
        this.active = GlobalActiveEnums.INACTIVE;
    }

    // [추가] 복구
    public void restore() {
        this.active = GlobalActiveEnums.ACTIVE;
    }

    // 고정 상태 변경
    public void setPinned(boolean pinned) {
        this.isPinned = pinned;
    }
}