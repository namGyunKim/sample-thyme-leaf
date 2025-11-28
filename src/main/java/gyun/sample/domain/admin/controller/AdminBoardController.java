package gyun.sample.domain.admin.controller;

import gyun.sample.domain.admin.service.AdminBoardService;
import gyun.sample.domain.board.enums.PostType;
import gyun.sample.domain.board.payload.request.PostListRequest;
import gyun.sample.domain.board.payload.response.PostListResponse;
import gyun.sample.global.enums.GlobalActiveEnums;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Tag(name = "AdminBoardController", description = "관리자 게시물 관리")
@Controller
@RequestMapping("/admin/board")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminBoardController {

    private final AdminBoardService adminBoardService;

    @Operation(summary = "전체 게시글 관리 목록")
    @GetMapping("/list")
    public String list(@Valid @ModelAttribute("request") PostListRequest request,
                       BindingResult bindingResult,
                       Model model) {

        if (bindingResult.hasErrors()) {
            // 에러 시 기본값으로 재설정 (Admin은 전체 조회를 기본으로 하므로 ACTIVE 필터 해제 등 고려 가능하나, Validator 로직상 ACTIVE가 기본일 수 있음)
            // 여기서는 전체 조회를 위해 active를 ALL로 두는 것이 좋지만, DTO 생성자 로직을 따름
            request = new PostListRequest(1, 10, "", null, GlobalActiveEnums.ALL, null);
        }

        Page<PostListResponse> postPage = adminBoardService.getAllPostsForAdmin(request);

        model.addAttribute("postPage", postPage);
        model.addAttribute("postTypes", PostType.values());
        model.addAttribute("activeTypes", GlobalActiveEnums.values());

        return "admin/board/list";
    }

    @Operation(summary = "게시글 비활성화 (Soft Delete)")
    @PostMapping("/deactivate/{id}")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminBoardService.deactivatePost(id);
        redirectAttributes.addFlashAttribute("message", "게시글이 비활성화(Soft Delete) 되었습니다.");
        return "redirect:/admin/board/list";
    }

    @Operation(summary = "게시글 복구")
    @PostMapping("/restore/{id}")
    public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminBoardService.restorePost(id);
        redirectAttributes.addFlashAttribute("message", "게시글이 복구(Active) 되었습니다.");
        return "redirect:/admin/board/list";
    }

    @Operation(summary = "게시글 완전 삭제")
    @PostMapping("/delete/{id}")
    public String hardDelete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminBoardService.hardDeletePost(id);
        redirectAttributes.addFlashAttribute("message", "게시글이 영구 삭제되었습니다.");
        return "redirect:/admin/board/list";
    }

    @Operation(summary = "공지글 토글")
    @PostMapping("/pin/{id}")
    public String togglePin(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminBoardService.togglePin(id);
        redirectAttributes.addFlashAttribute("message", "공지 상태가 변경되었습니다.");
        return "redirect:/admin/board/list";
    }
}