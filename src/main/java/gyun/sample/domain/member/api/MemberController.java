package gyun.sample.domain.member.api;

import gyun.sample.domain.account.enums.AccountRole;
import gyun.sample.domain.member.entity.Member;
import gyun.sample.domain.member.payload.dto.MemberListRequestDTO;
import gyun.sample.domain.member.payload.request.MemberCreateRequest;
import gyun.sample.domain.member.payload.request.MemberListRequest;
import gyun.sample.domain.member.payload.request.MemberRoleUpdateRequest;
import gyun.sample.domain.member.payload.request.MemberUpdateRequest;
import gyun.sample.domain.member.payload.response.DetailMemberResponse;
import gyun.sample.domain.member.payload.response.MemberListResponse;
import gyun.sample.domain.member.service.MemberStrategyFactory;
import gyun.sample.domain.member.service.read.ReadMemberService;
import gyun.sample.domain.member.service.write.WriteMemberService;
import gyun.sample.domain.member.validator.MemberCreateValidator;
import gyun.sample.domain.member.validator.MemberListValidator;
import gyun.sample.domain.member.validator.MemberRoleUpdateValidator;
import gyun.sample.domain.member.validator.MemberUserUpdateValidator;
import gyun.sample.global.security.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Tag(name = "MemberController", description = "회원 관리 (전략 패턴 적용)")
@Controller
@RequestMapping(value = "/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberStrategyFactory memberStrategyFactory;

    // Validators
    private final MemberCreateValidator memberCreateValidator;
    private final MemberListValidator memberListValidator;
    private final MemberUserUpdateValidator memberUserUpdateValidator;
    private final MemberRoleUpdateValidator memberRoleUpdateValidator;

    // Security Context Repository
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    // === InitBinders ===
    @InitBinder("memberCreateRequest")
    public void initBinderCreate(WebDataBinder dataBinder) {
        dataBinder.addValidators(memberCreateValidator);
    }

    @InitBinder("memberListRequest")
    public void initBinderList(WebDataBinder dataBinder) {
        dataBinder.addValidators(memberListValidator);
    }

    @InitBinder("memberUpdateRequest")
    public void initBinderUpdate(WebDataBinder dataBinder) {
        dataBinder.addValidators(memberUserUpdateValidator);
    }

    @InitBinder("memberRoleUpdateRequest")
    public void initBinderRoleUpdate(WebDataBinder dataBinder) {
        dataBinder.addValidators(memberRoleUpdateValidator);
    }

    // === Views & Actions ===

    @Operation(summary = "회원 생성 폼")
    @GetMapping(value = "/{role}/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public String createMemberForm(@PathVariable AccountRole role, Model model) {
        // [추가] 관리자(ADMIN/SUPER_ADMIN) 계정을 생성하려면, 요청자가 반드시 SUPER_ADMIN이어야 함
        validateManageAccess(role);

        if (!model.containsAttribute("memberCreateRequest")) {
            model.addAttribute("memberCreateRequest", new MemberCreateRequest(null, null, null, role, null));
        }
        model.addAttribute("role", role);
        return "member/create";
    }

    @Operation(summary = "회원 생성 처리")
    @PostMapping(value = "/{role}/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public String createMember(
            @PathVariable AccountRole role,
            @Valid @ModelAttribute("memberCreateRequest") MemberCreateRequest memberCreateRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        // [추가] 관리자 생성 권한 체크
        validateManageAccess(role);

        if (bindingResult.hasErrors()) {
            model.addAttribute("role", role);
            return "member/create";
        }

        WriteMemberService service = memberStrategyFactory.getWriteService(role);
        service.createMember(memberCreateRequest);

        redirectAttributes.addFlashAttribute("message", "회원이 성공적으로 생성되었습니다.");
        return "redirect:/member/" + role.name().toLowerCase() + "/list";
    }

    @Operation(summary = "회원 목록 조회")
    @GetMapping(value = "/{role}/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public String getMemberList(
            @PathVariable AccountRole role,
            @Valid @ModelAttribute("memberListRequest") MemberListRequest memberListRequest,
            BindingResult bindingResult,
            Model model) {

        // [추가] 관리자 목록 조회 권한 체크 (관리자 목록은 SUPER_ADMIN만 조회 가능)
        validateManageAccess(role);

        if (bindingResult.hasErrors()) {
            return "member/list";
        }

        ReadMemberService service = memberStrategyFactory.getReadService(role);
        Page<MemberListResponse> memberPage = service.getList(new MemberListRequestDTO(memberListRequest));

        model.addAttribute("role", role);
        model.addAttribute("memberPage", memberPage);
        model.addAttribute("request", memberListRequest);

        return "member/list";
    }

    @Operation(summary = "회원 상세 조회")
    @GetMapping(value = "/{role}/detail/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or (#role.name() == 'USER' and @memberGuard.checkCreateRole(#role))")
    public String getMemberDetail(
            @PathVariable AccountRole role,
            @PathVariable Long id,
            Model model) {

        // [추가] 관리자 상세 조회 권한 체크 (관리자 정보는 SUPER_ADMIN만 조회 가능)
        validateManageAccess(role);

        ReadMemberService service = memberStrategyFactory.getReadService(role);
        DetailMemberResponse response = service.getDetail(id);

        model.addAttribute("member", response);
        model.addAttribute("role", role);

        return "member/detail";
    }

    @Operation(summary = "회원 정보 수정 폼")
    @GetMapping(value = "/{role}/update/{id}")
    @PreAuthorize("isAuthenticated()")
    public String updateMemberForm(
            @PathVariable AccountRole role,
            @PathVariable Long id,
            @AuthenticationPrincipal PrincipalDetails principal,
            Model model) {

        ReadMemberService service = memberStrategyFactory.getReadService(role);
        DetailMemberResponse targetMember = service.getDetail(id);

        checkPermission(principal, targetMember.getProfile().role(), targetMember.getProfile().id());

        if (!model.containsAttribute("memberUpdateRequest")) {
            model.addAttribute("memberUpdateRequest", new MemberUpdateRequest(targetMember.getProfile().nickName(), null));
        }

        model.addAttribute("currentMember", targetMember);
        model.addAttribute("role", role);
        model.addAttribute("targetId", id);

        return "member/update";
    }

    @Operation(summary = "회원 정보 수정 처리")
    @PostMapping(value = "/{role}/update/{id}")
    @PreAuthorize("isAuthenticated()")
    public String updateMember(
            @PathVariable AccountRole role,
            @PathVariable Long id,
            @Valid @ModelAttribute("memberUpdateRequest") MemberUpdateRequest memberUpdateRequest,
            BindingResult bindingResult,
            @AuthenticationPrincipal PrincipalDetails principal,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        ReadMemberService readService = memberStrategyFactory.getReadService(role);
        DetailMemberResponse targetMember = readService.getDetail(id);

        checkPermission(principal, targetMember.getProfile().role(), targetMember.getProfile().id());

        if (bindingResult.hasErrors()) {
            model.addAttribute("currentMember", targetMember);
            model.addAttribute("role", role);
            model.addAttribute("targetId", id);
            return "member/update";
        }

        WriteMemberService writeService = memberStrategyFactory.getWriteService(role);
        writeService.updateMember(memberUpdateRequest, targetMember.getProfile().loginId());

        if (principal.getId().equals(id)) {
            refreshSession(request, response, principal.getUsername(), role);
        }

        redirectAttributes.addFlashAttribute("message", "정보가 성공적으로 수정되었습니다.");

        return principal.getId().equals(id) ? "redirect:/account/profile" :
                "redirect:/member/" + role.name().toLowerCase() + "/detail/" + id;
    }

    @Operation(summary = "회원 탈퇴/비활성화")
    @PostMapping(value = "/{role}/inactive/{id}")
    @PreAuthorize("isAuthenticated()")
    public String inactiveMember(
            @PathVariable AccountRole role,
            @PathVariable Long id,
            @AuthenticationPrincipal PrincipalDetails principal,
            RedirectAttributes redirectAttributes) {

        ReadMemberService readService = memberStrategyFactory.getReadService(role);
        DetailMemberResponse targetMember = readService.getDetail(id);

        checkPermission(principal, targetMember.getProfile().role(), targetMember.getProfile().id());

        WriteMemberService writeService = memberStrategyFactory.getWriteService(role);
        writeService.deActiveMember(targetMember.getProfile().loginId());

        if (principal.getId().equals(id)) {
            return "redirect:/logout";
        } else {
            redirectAttributes.addFlashAttribute("message", "해당 회원이 비활성화 처리되었습니다.");
            return "redirect:/member/" + role.name().toLowerCase() + "/list";
        }
    }

    @Operation(summary = "회원 등급 변경 폼")
    @GetMapping(value = "/{role}/role-update/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String updateMemberRoleForm(
            @PathVariable AccountRole role,
            @PathVariable Long id,
            Model model) {

        ReadMemberService service = memberStrategyFactory.getReadService(role);
        DetailMemberResponse targetMember = service.getDetail(id);

        if (!model.containsAttribute("memberRoleUpdateRequest")) {
            model.addAttribute("memberRoleUpdateRequest", new MemberRoleUpdateRequest(targetMember.getProfile().role()));
        }

        List<AccountRole> assignableRoles = Arrays.stream(AccountRole.values())
                .filter(r -> r != AccountRole.GUEST)
                .toList();

        model.addAttribute("targetMember", targetMember);
        model.addAttribute("assignableRoles", assignableRoles);
        model.addAttribute("role", role);
        model.addAttribute("targetId", id);

        return "member/role_update";
    }

    @Operation(summary = "회원 등급 변경 처리")
    @PostMapping(value = "/{role}/role-update/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String updateMemberRole(
            @PathVariable AccountRole role,
            @PathVariable Long id,
            @Valid @ModelAttribute("memberRoleUpdateRequest") MemberRoleUpdateRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        ReadMemberService readService = memberStrategyFactory.getReadService(role);
        DetailMemberResponse targetMember = readService.getDetail(id);

        if (bindingResult.hasErrors()) {
            List<AccountRole> assignableRoles = Arrays.stream(AccountRole.values())
                    .filter(r -> r != AccountRole.GUEST)
                    .toList();
            model.addAttribute("targetMember", targetMember);
            model.addAttribute("assignableRoles", assignableRoles);
            model.addAttribute("role", role);
            model.addAttribute("targetId", id);
            return "member/role_update";
        }

        WriteMemberService writeService = memberStrategyFactory.getWriteService(role);
        writeService.updateMemberRole(targetMember.getProfile().loginId(), request.role());

        redirectAttributes.addFlashAttribute("message", "회원 등급이 변경되었습니다.");

        String newRolePath = request.role().name().toLowerCase();
        return "redirect:/member/" + newRolePath + "/detail/" + id;
    }

    // === Helpers ===

    /**
     * 수정/삭제 권한 체크 (기존 로직)
     * - 본인은 수정 가능
     * - 최고 관리자(SUPER_ADMIN)는 모든 계정 수정 가능
     * - 관리자(ADMIN)는 사용자(USER)만 수정 가능
     */
    private void checkPermission(PrincipalDetails principal, AccountRole targetRole, Long targetId) {
        if (principal.getId().equals(targetId)) return; // 본인
        if (principal.getRole() == AccountRole.SUPER_ADMIN) return; // 최고 관리자
        if (principal.getRole() == AccountRole.ADMIN && targetRole == AccountRole.USER) return; // 관리자가 유저 관리

        throw new AccessDeniedException("해당 작업에 대한 권한이 없습니다.");
    }

    /**
     * [추가] 목록 조회 및 생성 권한 체크
     * - 관리자(ADMIN, SUPER_ADMIN) 계정을 조회하거나 생성하려는 경우,
     * 현재 로그인한 사용자가 반드시 SUPER_ADMIN 이어야 함.
     */
    private void validateManageAccess(AccountRole targetRole) {
        // 1. 대상이 관리자 계열인지 확인
        if (targetRole == AccountRole.ADMIN || targetRole == AccountRole.SUPER_ADMIN) {
            // 2. 현재 접속자 정보 확인
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            boolean isSuperAdmin = false;
            if (authentication != null && authentication.getPrincipal() instanceof PrincipalDetails principal) {
                isSuperAdmin = principal.getRole() == AccountRole.SUPER_ADMIN;
            }

            // 3. 권한 없으면 예외 발생
            if (!isSuperAdmin) {
                throw new AccessDeniedException("관리자 계정 관리는 최고 관리자(SUPER_ADMIN)만 가능합니다.");
            }
        }
    }

    private void refreshSession(HttpServletRequest request, HttpServletResponse response, String loginId, AccountRole role) {
        ReadMemberService readService = memberStrategyFactory.getReadService(role);
        Member updatedMember = readService.getByLoginIdAndRole(loginId, role);

        PrincipalDetails newPrincipal = new PrincipalDetails(updatedMember);
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                newPrincipal,
                newPrincipal.getPassword(),
                newPrincipal.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(newAuth);
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
    }
}