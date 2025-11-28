package gyun.sample.domain.log.api;

import gyun.sample.domain.log.payload.request.MemberLogRequest;
import gyun.sample.domain.log.payload.request.PostLogRequest;
import gyun.sample.domain.log.payload.response.MemberLogResponse;
import gyun.sample.domain.log.payload.response.PostLogResponse;
import gyun.sample.domain.log.service.read.ReadMemberLogService;
import gyun.sample.domain.log.service.read.ReadPostLogService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Tag(name = "LogController", description = "시스템 로그 관리")
@Controller
@RequestMapping("/log")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class LogController {

    private final ReadMemberLogService readMemberLogService;
    private final ReadPostLogService readPostLogService;

    @Operation(summary = "회원 활동 로그 목록 페이지")
    @GetMapping("/member")
    public String memberLogList(
            @Valid @ModelAttribute("request") MemberLogRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("logPage", Page.empty());
            return "log/member/list";
        }

        Page<MemberLogResponse> logPage = readMemberLogService.getMemberLogs(request);
        model.addAttribute("logPage", logPage);

        return "log/member/list";
    }

    @Operation(summary = "게시글 활동 로그 목록 페이지")
    @GetMapping("/board")
    public String boardLogList(
            @Valid @ModelAttribute("request") PostLogRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("logPage", Page.empty());
            return "log/board/list";
        }

        Page<PostLogResponse> logPage = readPostLogService.getPostLogs(request);
        model.addAttribute("logPage", logPage);

        return "log/board/list"; // 뷰 템플릿 경로
    }
}