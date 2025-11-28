package gyun.sample.domain.board.service.read;

import gyun.sample.domain.board.payload.request.PostListRequest;
import gyun.sample.domain.board.payload.response.PostDetailResponse;
import gyun.sample.domain.board.payload.response.PostListResponse;
import org.springframework.data.domain.Page;

public interface ReadPostService {

    /**
     * 게시글 상세 조회 (게시글 내용 + 댓글 목록)
     *
     * @param postId 조회할 게시글 ID
     * @return 게시글 상세 DTO
     */
    PostDetailResponse getPostDetail(Long postId, long currentUserId);

    Page<PostListResponse> getPostAll(PostListRequest postListRequest);
}