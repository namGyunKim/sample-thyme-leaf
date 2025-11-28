package gyun.sample.domain.board.service.write;

import gyun.sample.domain.board.payload.request.PostCreateRequest;
import gyun.sample.domain.board.payload.request.PostUpdateRequest;

public interface WritePostService {

    /**
     * 게시글을 저장하는 실제 로직을 수행합니다.
     *
     * @param request 게시글 생성 요청 DTO
     * @return 생성된 게시글 ID
     */
    long save(PostCreateRequest request, long userId);

    /**
     * 게시글 핀 상태를 업데이트합니다.
     *
     * @param postId 게시글 ID
     * @param userId 현재 사용자 ID (작업 수행자)
     * @param pin    고정 여부
     */
    void pinPost(long postId, long userId, boolean pin);

    boolean delete(long postId, long userId);

    void update(long postId, PostUpdateRequest request, long userId);

    /**
     * 게시글 좋아요 토글 (증가/취소)
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 변경된 좋아요 수
     */
    int likePost(long postId, long userId);

    /**
     * 조회수 증가
     * @param postId 게시글 ID
     */
    void increaseViewCount(long postId);
}