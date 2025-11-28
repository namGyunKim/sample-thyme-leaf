package gyun.sample.domain.board.service.read;

import com.querydsl.core.types.Predicate;
import gyun.sample.domain.board.entity.Post;
import gyun.sample.domain.board.enums.PostType;
import gyun.sample.domain.board.payload.request.PostListRequest;
import gyun.sample.domain.board.payload.response.PostDetailResponse;
import gyun.sample.domain.board.payload.response.PostListResponse;
import gyun.sample.domain.board.repository.PostLikeRepository;
import gyun.sample.domain.board.repository.PostRepository;
import gyun.sample.domain.board.repository.PostSpecification;
import gyun.sample.global.exception.GlobalException;
import gyun.sample.global.exception.enums.ErrorCode;
import gyun.sample.global.utils.UtilService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
public abstract class AbstractReadPostService implements ReadPostService {

    protected final PostRepository postRepository;
    protected final PostLikeRepository postLikeRepository;

    public abstract PostType getPostType();

    @Override
    public Page<PostListResponse> getPostAll(PostListRequest request) {
        Sort sort = Sort.by(Sort.Direction.DESC, "isPinned", "createdAt");
        Pageable pageable = PageRequest.of(request.page() - 1, request.size(), sort);

        Predicate predicate = PostSpecification.searchPost(request, getPostType());

        // [성능 개선] N+1 문제를 해결하기 위해 fetch join이 적용된 메서드 호출
        // 기존: postRepository.findAll(predicate, pageable)
        Page<Post> posts = postRepository.findAllWithAuthor(predicate, pageable);

        return posts.map(PostListResponse::new);
    }

    @Override
    public PostDetailResponse getPostDetail(Long postId, long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GlobalException(ErrorCode.PAGE_NOT_EXIST));

        if (post.getPostType() != getPostType()) {
            throw new GlobalException(ErrorCode.INVALID_PARAMETER, "해당 게시판의 글이 아닙니다.");
        }

        boolean isLiked = false;
        if (currentUserId != 0) {
            isLiked = postLikeRepository.existsByPostIdAndMemberId(postId, currentUserId);
        }

        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getNickName(),
                post.getAuthor().getId(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getPostType().name(),
                isLiked,
                UtilService.formattedTime(post.getCreatedAt()),
                // [SEO] ISO 날짜 포맷 추가
                post.getCreatedAt() != null ? post.getCreatedAt().toString() : "",
                // [SEO] 썸네일 추출 로직 적용
                extractThumbnail(post.getContent())
        );
    }

    private String extractThumbnail(String content) {
        if (content == null || !content.contains("<img")) return null;
        try {
            int srcStart = content.indexOf("src=\"");
            if (srcStart == -1) return null;
            int srcEnd = content.indexOf("\"", srcStart + 5);
            if (srcEnd == -1) return null;
            return content.substring(srcStart + 5, srcEnd);
        } catch (Exception e) {
            return null;
        }
    }
}