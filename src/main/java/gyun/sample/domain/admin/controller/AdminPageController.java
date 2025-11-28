package gyun.sample.domain.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "AdminPageController", description = "관리자 전용 페이지 컨트롤러")
@Controller
@RequestMapping("/admin")
public class AdminPageController {

    @Operation(summary = "관리자 로그인 페이지")
    @GetMapping("/login")
    public String adminLoginForm(@RequestParam(value = "error", required = false) String error,
                                 @RequestParam(value = "logout", required = false) String logout,
                                 Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "아이디 또는 비밀번호를 확인해주세요. (관리자 권한 필요)");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "관리자 로그아웃 되었습니다.");
        }
        return "admin/login";
    }
}