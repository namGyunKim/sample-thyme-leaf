package gyun.sample.domain.member.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import gyun.sample.domain.account.enums.AccountRole;
import gyun.sample.domain.member.payload.dto.MemberListRequestDTO;
import gyun.sample.global.enums.GlobalActiveEnums;
import gyun.sample.global.enums.GlobalFilterEnums;
import org.springframework.util.StringUtils;

import java.util.List;

import static gyun.sample.domain.member.entity.QMember.member;

/**
 * QueryDSL을 이용한 동적 쿼리 조건(Predicate) 생성
 * 기존 JPA Specification을 대체합니다.
 */
public class MemberSpecification {

    public static Predicate searchMember(MemberListRequestDTO request, List<AccountRole> roles) {
        BooleanBuilder builder = new BooleanBuilder();

        // 1. 권한(Role) 필터링 (IN 절)
        if (roles != null && !roles.isEmpty()) {
            builder.and(member.role.in(roles));
        }

        // 2. 활성화 상태(Active) 필터링
        if (request.active() != null && request.active() != GlobalActiveEnums.ALL) {
            builder.and(member.active.eq(request.active()));
        }

        // 3. 검색어(SearchWord) 및 필터(Filter) 처리
        if (StringUtils.hasText(request.searchWord())) {
            String keyword = request.searchWord();
            GlobalFilterEnums filter = request.filter() != null ? request.filter() : GlobalFilterEnums.ALL;

            switch (filter) {
                case LOGIN_ID -> builder.and(member.loginId.containsIgnoreCase(keyword));
                case NICK_NAME -> builder.and(member.nickName.containsIgnoreCase(keyword));
                // ALL: 로그인 ID 또는 닉네임 중 하나라도 포함되면 검색 (OR 조건)
                default -> builder.and(
                        member.loginId.containsIgnoreCase(keyword)
                                .or(member.nickName.containsIgnoreCase(keyword))
                );
            }
        }

        return builder;
    }
}