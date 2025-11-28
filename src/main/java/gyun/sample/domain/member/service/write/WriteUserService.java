package gyun.sample.domain.member.service.write;

import gyun.sample.domain.account.enums.AccountRole;
import gyun.sample.domain.aws.enums.ImageType;
import gyun.sample.domain.aws.enums.UploadDirect;
import gyun.sample.domain.aws.service.implement.S3MemberService;
import gyun.sample.domain.log.enums.LogType;
import gyun.sample.domain.log.event.MemberActivityEvent;
import gyun.sample.domain.member.entity.Member;
import gyun.sample.domain.member.entity.MemberImage;
import gyun.sample.domain.member.enums.MemberType;
import gyun.sample.domain.member.payload.request.MemberCreateRequest;
import gyun.sample.domain.member.payload.request.MemberUpdateRequest;
import gyun.sample.domain.member.repository.MemberRepository;
import gyun.sample.domain.member.service.read.ReadUserService;
import gyun.sample.domain.social.google.service.GoogleSocialService;
import gyun.sample.global.payload.response.GlobalCreateResponse;
import gyun.sample.global.payload.response.GlobalInactiveResponse;
import gyun.sample.global.payload.response.GlobalUpdateResponse;
import gyun.sample.global.security.PrincipalDetails;
import gyun.sample.global.utils.UtilService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class WriteUserService extends AbstractWriteMemberService {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final ReadUserService readUserService;
    private final S3MemberService s3MemberService;
    private final GoogleSocialService googleSocialService;
    private final ApplicationEventPublisher eventPublisher;
    private final HttpServletRequest httpServletRequest;

    @Override
    public List<AccountRole> getSupportedRoles() {
        return List.of(AccountRole.USER);
    }

    @Override
    public GlobalCreateResponse createMember(MemberCreateRequest request) {
        Member createdMember = new Member(request);
        Member member = memberRepository.save(createdMember);
        member.updatePassword(passwordEncoder.encode(request.password()));
        publishLog(member.getLoginId(), member.getId(), LogType.JOIN, "일반 회원 가입");
        return new GlobalCreateResponse(member.getId());
    }

    @Override
    public GlobalUpdateResponse updateMember(MemberUpdateRequest memberUpdateRequest, String loginId) {
        // 1. 조회
        Member member = readUserService.getByLoginIdAndRole(loginId, AccountRole.USER);

        // 2. 기본 정보 변경 (닉네임 등)
        member.update(memberUpdateRequest);

        // 3. [추가] 비밀번호 변경 (일반 계정 & 비밀번호 값이 있을 때만)
        if (member.getMemberType() == MemberType.GENERAL && StringUtils.hasText(memberUpdateRequest.password())) {
            member.updatePassword(passwordEncoder.encode(memberUpdateRequest.password()));
            publishLog(loginId, member.getId(), LogType.PASSWORD_CHANGE, "비밀번호 변경");
        }

        // 4. 로그 발행
        publishLog(member.getLoginId(), member.getId(), LogType.UPDATE, "회원 정보 수정");

        return new GlobalUpdateResponse(member.getId());
    }

    @Override
    public GlobalInactiveResponse deActiveMember(String loginId) {
        Member member = readUserService.getByLoginIdAndRole(loginId, AccountRole.USER);

        if (member.getMemberType() == MemberType.GOOGLE) {
            googleSocialService.unlink(member);
        }

        member.withdraw();

        if (!member.getMemberImages().isEmpty()) {
            List<String> fileNames = member.getMemberImages().stream()
                    .filter(mi -> mi.getUploadDirect() == UploadDirect.MEMBER_PROFILE)
                    .map(MemberImage::getFileName)
                    .toList();

            s3MemberService.deleteImages(fileNames, ImageType.MEMBER_PROFILE, member.getId());
            member.getMemberImages().clear();
        }

        publishLog(loginId, member.getId(), LogType.INACTIVE, "회원 탈퇴 처리");
        return new GlobalInactiveResponse(member.getId());
    }

    @Override
    public void updateMemberRole(String loginId, AccountRole newRole) {
        Member member = readUserService.getByLoginIdAndRole(loginId, AccountRole.USER);
        AccountRole oldRole = member.getRole();
        member.changeRole(newRole);
        publishLog(member.getLoginId(), member.getId(), LogType.UPDATE, "권한 변경: " + oldRole + " -> " + newRole);
    }

    private void publishLog(String targetId, Long memberId, LogType type, String details) {
        // executorId 조회 로직 제거
        eventPublisher.publishEvent(MemberActivityEvent.of(
                targetId, memberId, type, details, UtilService.getClientIp(httpServletRequest)
        ));
    }

    private String getExecutorId(String defaultId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof PrincipalDetails principal) {
            return principal.getUsername();
        }
        return defaultId;
    }
}