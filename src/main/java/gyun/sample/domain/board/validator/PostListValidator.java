package gyun.sample.domain.board.validator;

import gyun.sample.domain.account.enums.AccountRole;
import gyun.sample.domain.board.payload.request.PostListRequest;
import gyun.sample.global.enums.GlobalActiveEnums;
import gyun.sample.global.security.PrincipalDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class PostListValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return PostListRequest.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        PostListRequest request = (PostListRequest) target;

        // 현재 사용자 권한 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = false;

        if (authentication != null && authentication.getPrincipal() instanceof PrincipalDetails principal) {
            AccountRole role = principal.getRole();
            isAdmin = (role == AccountRole.ADMIN || role == AccountRole.SUPER_ADMIN);
        }

        // 관리자가 아닌 경우
        if (!isAdmin) {
            // 1. 비활성(삭제된) 게시글 조회 불가
            if (request.active() != GlobalActiveEnums.ACTIVE) {
                errors.rejectValue("active", "active.invalid", "일반 사용자는 활성 게시글만 조회할 수 있습니다.");
            }
            // 2. 게시판 타입 교차 조회 불가 (Controller에서 PathVariable로 강제하므로 여기선 패스하거나 체크)
        }
    }
}