package gyun.sample.domain.board.api;

import gyun.sample.domain.account.enums.AccountRole;
import gyun.sample.domain.account.payload.dto.CurrentAccountDTO;
import gyun.sample.domain.board.enums.PostType;
import gyun.sample.domain.board.payload.request.PostCreateRequest;
import gyun.sample.domain.board.payload.request.PostListRequest;
import gyun.sample.domain.board.payload.request.PostUpdateRequest;
import gyun.sample.domain.board.payload.response.PostDetailResponse;
import gyun.sample.domain.board.payload.response.PostListResponse;
import gyun.sample.domain.board.service.PostStrategyFactory;
import gyun.sample.domain.board.service.read.ReadPostService;
import gyun.sample.domain.board.service.write.WritePostService;
import gyun.sample.domain.board.validator.PostListValidator;
import gyun.sample.domain.board.validator.PostValidator;
import gyun.sample.domain.log.enums.LogType;
import gyun.sample.domain.log.event.PostActivityEvent;
import gyun.sample.global.annotaion.CurrentAccount;
import gyun.sample.global.exception.GlobalException;
import gyun.sample.global.exception.enums.ErrorCode;
import gyun.sample.global.utils.UtilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;

@Slf4j
@Tag(name = "BoardController", description = "게시판 컨트롤러")
@Controller
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {

    private final PostStrategyFactory postStrategyFactory;
    private final PostValidator postValidator;
    private final PostListValidator postListValidator; // 추가
    private final ApplicationEventPublisher eventPublisher;

    @InitBinder("postCreateRequest")
    public void initBinderCreate(WebDataBinder binder) {
        binder.addValidators(postValidator);
    }

    @InitBinder("postUpdateRequest")
    public void initBinderUpdate(WebDataBinder binder) {
        binder.addValidators(postValidator);
    }

    // [추가] 목록 요청 검증기 등록
    @InitBinder("postListRequest")
    public void initBinderList(WebDataBinder binder) {
        binder.addValidators(postListValidator);
    }

    @Operation(summary = "게시글 목록 조회")
    @GetMapping("/{type}/list")
    public String list(
            @PathVariable String type,
            @Valid @ModelAttribute("postListRequest") PostListRequest request,
            BindingResult bindingResult,
            Model model) {

        PostType postType = getValidPostType(type);

        // 유효성 검사 실패 시 (예: 일반 유저가 INACTIVE 조회 시도)
        if (bindingResult.hasErrors()) {
            // 기본값으로 재설정하여 조회하거나 에러 페이지로 보냄. 여기서는 기본값으로 재조회 로직
            request = new PostListRequest(1, 10, "", null, null, null);
        }

        ReadPostService readService = postStrategyFactory.getReadService(postType);
        Page<PostListResponse> postPage = readService.getPostAll(request);

        model.addAttribute("postPage", postPage);
        model.addAttribute("postType", postType);
        model.addAttribute("typeStr", type.toLowerCase());

        return "board/list";
    }

    @Operation(summary = "게시글 상세 조회")
    @GetMapping("/{type}/detail/{id}")
    public String detail(
            @PathVariable String type,
            @PathVariable Long id,
            @CurrentAccount CurrentAccountDTO currentAccount,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        PostType postType = getValidPostType(type);

        // 1. 상세 데이터 조회 (먼저 조회하여 로그에 필요한 정보를 확보)
        ReadPostService readService = postStrategyFactory.getReadService(postType);
        PostDetailResponse post = readService.getPostDetail(id, currentAccount.id());

        // 2. 조회수 증가 및 로그 발행 (쿠키 기반 중복 방지)
        // [수정] 조회수가 실제 증가할 때만 로그를 발행하도록 로직 변경
        handleViewCountAndLog(id, postType, post, currentAccount, request, response);

        model.addAttribute("post", post);
        model.addAttribute("postType", postType);
        model.addAttribute("typeStr", type.toLowerCase());
        model.addAttribute("currentUser", currentAccount);

        return "board/detail";
    }

    @Operation(summary = "게시글 작성 폼")
    @GetMapping("/{type}/write")
    @PreAuthorize("isAuthenticated()")
    public String writeForm(
            @PathVariable String type,
            Model model) {

        PostType postType = getValidPostType(type);
        model.addAttribute("postCreateRequest", new PostCreateRequest(null, null, postType));
        model.addAttribute("postType", postType);
        model.addAttribute("typeStr", type.toLowerCase());

        return "board/write";
    }

    @Operation(summary = "게시글 작성 처리")
    @PostMapping("/{type}/write")
    @PreAuthorize("isAuthenticated()")
    public String write(
            @PathVariable String type,
            @Valid @ModelAttribute("postCreateRequest") PostCreateRequest request,
            BindingResult bindingResult,
            @CurrentAccount CurrentAccountDTO currentAccount,
            RedirectAttributes redirectAttributes,
            Model model) {

        PostType postType = getValidPostType(type);

        if (bindingResult.hasErrors()) {
            model.addAttribute("postType", postType);
            model.addAttribute("typeStr", type.toLowerCase());
            return "board/write";
        }

        WritePostService writeService = postStrategyFactory.getWriteService(postType);
        writeService.save(request, currentAccount.id());

        redirectAttributes.addFlashAttribute("message", "게시글이 등록되었습니다.");
        return "redirect:/board/" + type.toLowerCase() + "/list";
    }

    @Operation(summary = "게시글 수정 폼")
    @GetMapping("/{type}/update/{id}")
    @PreAuthorize("isAuthenticated()")
    public String updateForm(
            @PathVariable String type,
            @PathVariable Long id,
            @CurrentAccount CurrentAccountDTO currentAccount,
            Model model) {

        PostType postType = getValidPostType(type);
        ReadPostService readService = postStrategyFactory.getReadService(postType);

        PostDetailResponse post = readService.getPostDetail(id, currentAccount.id());

        if (!post.authorUserId().equals(currentAccount.id()) &&
                currentAccount.role() != AccountRole.SUPER_ADMIN) {
            throw new GlobalException(ErrorCode.ACCESS_DENIED);
        }

        model.addAttribute("postUpdateRequest", new PostUpdateRequest(post.title(), post.content()));
        model.addAttribute("postType", postType);
        model.addAttribute("typeStr", type.toLowerCase());
        model.addAttribute("postId", id);

        return "board/update";
    }

    @Operation(summary = "게시글 수정 처리")
    @PostMapping("/{type}/update/{id}")
    @PreAuthorize("isAuthenticated()")
    public String update(
            @PathVariable String type,
            @PathVariable Long id,
            @Valid @ModelAttribute("postUpdateRequest") PostUpdateRequest request,
            BindingResult bindingResult,
            @CurrentAccount CurrentAccountDTO currentAccount,
            RedirectAttributes redirectAttributes,
            Model model) {

        PostType postType = getValidPostType(type);

        if (bindingResult.hasErrors()) {
            model.addAttribute("postType", postType);
            model.addAttribute("typeStr", type.toLowerCase());
            model.addAttribute("postId", id);
            return "board/update";
        }

        WritePostService writeService = postStrategyFactory.getWriteService(postType);
        writeService.update(id, request, currentAccount.id());

        redirectAttributes.addFlashAttribute("message", "게시글이 수정되었습니다.");
        return "redirect:/board/" + type.toLowerCase() + "/detail/" + id;
    }

    @Operation(summary = "게시글 삭제")
    @PostMapping("/{type}/delete/{id}")
    @PreAuthorize("isAuthenticated()")
    public String delete(
            @PathVariable String type,
            @PathVariable Long id,
            @CurrentAccount CurrentAccountDTO currentAccount,
            RedirectAttributes redirectAttributes) {

        PostType postType = getValidPostType(type);
        WritePostService writeService = postStrategyFactory.getWriteService(postType);
        writeService.delete(id, currentAccount.id());

        redirectAttributes.addFlashAttribute("message", "게시글이 삭제되었습니다.");
        return "redirect:/board/" + type.toLowerCase() + "/list";
    }

    @Operation(summary = "게시글 좋아요")
    @PostMapping("/{type}/like/{id}")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Integer> like(
            @PathVariable String type,
            @PathVariable Long id,
            @CurrentAccount CurrentAccountDTO currentAccount) {

        PostType postType = getValidPostType(type);
        WritePostService writeService = postStrategyFactory.getWriteService(postType);

        int newLikeCount = writeService.likePost(id, currentAccount.id());

        return ResponseEntity.ok(newLikeCount);
    }

    private PostType getValidPostType(String typeStr) {
        PostType postType = PostType.create(typeStr);
        if (postType == null) {
            throw new GlobalException(ErrorCode.INVALID_PARAMETER, "존재하지 않는 게시판 타입입니다.");
        }
        return postType;
    }

    // [수정] 조회수 증가 및 로그 발행 통합 메소드
    private void handleViewCountAndLog(Long postId, PostType postType, PostDetailResponse post,
                                       CurrentAccountDTO account,
                                       HttpServletRequest request, HttpServletResponse response) {
        String cookieName = "post_view_" + postId;
        boolean alreadyViewed = false;

        if (request.getCookies() != null) {
            alreadyViewed = Arrays.stream(request.getCookies())
                    .anyMatch(cookie -> cookie.getName().equals(cookieName));
        }

        if (!alreadyViewed) {
            // 1. 조회수 증가
            WritePostService writeService = postStrategyFactory.getWriteService(postType);
            writeService.increaseViewCount(postId);

            // 2. 쿠키 발급
            Cookie newCookie = new Cookie(cookieName, "true");
            newCookie.setPath("/");
            newCookie.setMaxAge(60 * 60 * 24); // 24시간
            response.addCookie(newCookie);

            // 3. 로그 발행 (최초 조회 시에만 발행하여 DB 부하 감소)
            publishViewLog(post, account, request);
        }
    }

    private void publishViewLog(PostDetailResponse post, CurrentAccountDTO account, HttpServletRequest request) {
        String executorId = account.id() == 0L ? "GUEST" : account.loginId();
        String clientIp = UtilService.getClientIp(request);

        eventPublisher.publishEvent(PostActivityEvent.of(
                post.postId(),
                post.title(),
                executorId,
                LogType.POST_VIEW,
                "게시글 조회 (IP: " + clientIp + ")",
                clientIp
        ));
    }
}