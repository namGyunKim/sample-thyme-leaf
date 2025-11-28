package gyun.sample.domain.admin.service;

import com.querydsl.core.types.Predicate;
import gyun.sample.domain.board.entity.Post;
import gyun.sample.domain.board.payload.request.PostListRequest;
import gyun.sample.domain.board.payload.response.PostListResponse;
import gyun.sample.domain.board.repository.PostRepository;
import gyun.sample.domain.board.repository.PostSpecification;
import gyun.sample.domain.log.enums.LogType;
import gyun.sample.domain.log.event.PostActivityEvent;
import gyun.sample.global.exception.GlobalException;
import gyun.sample.global.exception.enums.ErrorCode;
import gyun.sample.global.security.PrincipalDetails;
import gyun.sample.global.utils.UtilService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminBoardService {

    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final HttpServletRequest httpServletRequest;

    /**
     * 관리자용 전체 게시글 조회
     */
    @Transactional(readOnly = true)
    public Page<PostListResponse> getAllPostsForAdmin(PostListRequest request) {
        Sort sort = Sort.by(Sort.Direction.DESC, "isPinned", "createdAt");
        Pageable pageable = PageRequest.of(request.page() - 1, request.size(), sort);

        // PostType이 null이면 전체 조회
        Predicate predicate = PostSpecification.searchPost(request, null);

        Page<Post> posts = postRepository.findAllWithAuthor(predicate, pageable);
        return posts.map(PostListResponse::new);
    }

    /**
     * 게시글 비활성화 (ACTIVE -> INACTIVE, Soft Delete)
     * [추가] 관리자 권한으로 강제 비활성화
     */
    public void deactivatePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GlobalException(ErrorCode.PAGE_NOT_EXIST));

        post.delete(); // Soft Delete (active = INACTIVE)
        publishAdminLog(postId, post.getTitle(), LogType.POST_DELETE, "관리자 게시글 비활성화(Soft Delete)");
    }

    /**
     * 게시글 복구 (INACTIVE -> ACTIVE)
     */
    public void restorePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GlobalException(ErrorCode.PAGE_NOT_EXIST));

        post.restore();
        publishAdminLog(postId, post.getTitle(), LogType.POST_UPDATE, "관리자 게시글 복구");
    }

    /**
     * 게시글 완전 삭제 (Hard Delete)
     */
    public void hardDeletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GlobalException(ErrorCode.PAGE_NOT_EXIST));

        String title = post.getTitle(); // 삭제 전 제목 백업
        postRepository.delete(post);

        publishAdminLog(postId, title, LogType.POST_DELETE, "관리자 완전 삭제(Hard Delete)");
    }

    /**
     * 공지글 토글
     */
    public void togglePin(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GlobalException(ErrorCode.PAGE_NOT_EXIST));

        boolean newStatus = !post.isPinned();
        post.setPinned(newStatus);

        publishAdminLog(postId, post.getTitle(), LogType.POST_UPDATE, newStatus ? "공지 등록" : "공지 해제");
    }

    private void publishAdminLog(Long postId, String title, LogType logType, String details) {
        String executorId = getExecutorId();
        eventPublisher.publishEvent(PostActivityEvent.of(
                postId, title, executorId, logType, details, UtilService.getClientIp(httpServletRequest)
        ));
    }

    private String getExecutorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof PrincipalDetails principal) {
            return principal.getUsername();
        }
        return "ADMIN";
    }
}