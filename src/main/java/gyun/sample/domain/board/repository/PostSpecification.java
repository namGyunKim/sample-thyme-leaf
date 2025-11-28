package gyun.sample.domain.board.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import gyun.sample.domain.board.enums.PostType;
import gyun.sample.domain.board.payload.request.PostListRequest;
import gyun.sample.global.enums.GlobalActiveEnums;
import gyun.sample.global.enums.GlobalFilterEnums;
import org.springframework.util.StringUtils;

import static gyun.sample.domain.board.entity.QPost.post;

public class PostSpecification {

    /**
     * 게시글 검색 조건 생성
     *
     * @param request       검색 요청 DTO
     * @param fixedPostType 컨트롤러에서 고정된 게시판 타입 (null이면 request의 searchPostType 사용 - 관리자용)
     */
    public static Predicate searchPost(PostListRequest request, PostType fixedPostType) {
        BooleanBuilder builder = new BooleanBuilder();

        // 1. 게시판 타입 필터링
        // fixedPostType이 있으면 그것을 강제(일반 사용자용), 없으면 request의 조건 사용(관리자 전체 조회용)
        if (fixedPostType != null) {
            builder.and(post.postType.eq(fixedPostType));
        } else if (request.searchPostType() != null) {
            builder.and(post.postType.eq(request.searchPostType()));
        }

        // 2. 활성 상태 필터링
        // 관리자는 ALL이나 INACTIVE를 요청할 수 있음. 일반 사용자는 Validator에서 ACTIVE로 강제됨.
        if (request.active() != null && request.active() != GlobalActiveEnums.ALL) {
            builder.and(post.active.eq(request.active()));
        }

        // 3. 검색어 처리
        if (StringUtils.hasText(request.searchWord())) {
            String keyword = request.searchWord();
            GlobalFilterEnums filter = request.filter() != null ? request.filter() : GlobalFilterEnums.ALL;

            switch (filter) {
                case TITLE -> builder.and(post.title.containsIgnoreCase(keyword));
                case CONTENT -> builder.and(post.content.containsIgnoreCase(keyword));
                case NICK_NAME -> builder.and(post.author.nickName.containsIgnoreCase(keyword));
                // ALL: 제목 + 내용 + 작성자
                default -> builder.and(
                        post.title.containsIgnoreCase(keyword)
                                .or(post.content.containsIgnoreCase(keyword))
                                .or(post.author.nickName.containsIgnoreCase(keyword))
                );
            }
        }

        return builder;
    }
}