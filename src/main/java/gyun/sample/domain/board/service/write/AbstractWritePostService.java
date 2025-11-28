package gyun.sample.domain.board.service.write;

import gyun.sample.domain.account.enums.AccountRole;
import gyun.sample.domain.board.entity.Post;
import gyun.sample.domain.board.entity.PostLike;
import gyun.sample.domain.board.enums.PostType;
import gyun.sample.domain.board.payload.request.PostCreateRequest;
import gyun.sample.domain.board.payload.request.PostUpdateRequest;
import gyun.sample.domain.board.repository.PostLikeRepository;
import gyun.sample.domain.board.repository.PostRepository;
import gyun.sample.domain.log.enums.LogType;
import gyun.sample.domain.log.event.PostActivityEvent;
import gyun.sample.domain.member.entity.Member;
import gyun.sample.domain.member.repository.MemberRepository;
import gyun.sample.global.exception.GlobalException;
import gyun.sample.global.exception.enums.ErrorCode;
import gyun.sample.global.security.PrincipalDetails;
import gyun.sample.global.utils.UtilService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Transactional
public abstract class AbstractWritePostService implements WritePostService {

    protected final PostRepository postRepository;
    protected final MemberRepository memberRepository;
    protected final PostLikeRepository postLikeRepository;

    protected final ApplicationEventPublisher eventPublisher;
    protected final HttpServletRequest httpServletRequest;

    public abstract PostType getPostType();

    @Override
    public long save(PostCreateRequest request, long userId) {
        Member author = memberRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.MEMBER_NOT_EXIST));

        Post post = new Post(request.title(), request.content(), getPostType(), author);
        Post savedPost = postRepository.save(post);

        // [최적화] 이미 조회된 Author 정보를 사용하여 로그 발행 (DB 재조회 방지)
        publishPostLogWithAuthor(savedPost, author.getLoginId(), LogType.POST_CREATE, "게시글 작성");

        return savedPost.getId();
    }

    @Override
    public void update(long postId, PostUpdateRequest request, long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GlobalException(ErrorCode.PAGE_NOT_EXIST));

        validateOwner(post, userId);
        post.update(request);

        // [최적화] SecurityContext에서 수행자 ID 조회
        publishPostLog(post, LogType.POST_UPDATE, "게시글 수정");
    }

    @Override
    public boolean delete(long postId, long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GlobalException(ErrorCode.PAGE_NOT_EXIST));

        validateOwner(post, userId);
        post.delete(); // Soft Delete

        // [최적화] SecurityContext에서 수행자 ID 조회
        publishPostLog(post, LogType.POST_DELETE, "게시글 삭제 (Soft Delete)");

        return true;
    }

    @Override
    public void pinPost(long postId, long userId, boolean pin) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.MEMBER_NOT_EXIST));

        if (member.getRole() != AccountRole.ADMIN && member.getRole() != AccountRole.SUPER_ADMIN) {
            throw new GlobalException(ErrorCode.ACCESS_DENIED, "관리자만 공지글을 설정할 수 있습니다.");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GlobalException(ErrorCode.PAGE_NOT_EXIST));
        post.setPinned(pin);

        // [최적화] SecurityContext에서 수행자 ID 조회 (이미 조회된 member가 있지만, 일관성을 위해 Context 사용)
        publishPostLog(post, LogType.POST_UPDATE, pin ? "공지글 설정" : "공지글 해제");
    }

    @Override
    public int likePost(long postId, long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GlobalException(ErrorCode.PAGE_NOT_EXIST));

        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.MEMBER_NOT_EXIST));

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndMemberId(postId, userId);

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            post.decreaseLikeCount();
        } else {
            postLikeRepository.save(new PostLike(post, member));
            post.increaseLikeCount();
        }

        return post.getLikeCount();
    }

    @Override
    public void increaseViewCount(long postId) {
        if (!postRepository.existsById(postId)) {
            throw new GlobalException(ErrorCode.PAGE_NOT_EXIST);
        }
        postRepository.increaseViewCount(postId);
        // 조회수 로그는 Controller에서 별도 처리
    }

    private void validateOwner(Post post, long userId) {
        if (post.getAuthor().getId() != userId) {
            // 슈퍼 관리자 삭제 권한 등을 위한 확장 포인트
            throw new GlobalException(ErrorCode.ACCESS_DENIED, "작성자만 수정/삭제할 수 있습니다.");
        }
    }

    // [최적화] SecurityContextHolder를 사용하여 DB 조회 없이 수행자 ID 획득
    private void publishPostLog(Post post, LogType logType, String details) {
        String executorId = getExecutorId();
        publishEvent(post, executorId, logType, details);
    }

    // 작성자 정보가 이미 있는 경우 사용 (save 메소드용)
    private void publishPostLogWithAuthor(Post post, String authorLoginId, LogType logType, String details) {
        publishEvent(post, authorLoginId, logType, details);
    }

    private void publishEvent(Post post, String executorId, LogType logType, String details) {
        eventPublisher.publishEvent(PostActivityEvent.of(
                post.getId(),
                post.getTitle(),
                executorId,
                logType,
                details,
                UtilService.getClientIp(httpServletRequest)
        ));
    }

    private String getExecutorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof PrincipalDetails principal) {
            return principal.getUsername();
        }
        return "UNKNOWN";
    }
}