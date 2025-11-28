package gyun.sample.domain.board.payload.request;

import gyun.sample.domain.board.enums.PostType;
import gyun.sample.global.enums.GlobalActiveEnums;
import gyun.sample.global.enums.GlobalFilterEnums;
import io.swagger.v3.oas.annotations.media.Schema;

public record PostListRequest(
        @Schema(description = "페이지 번호", example = "1")
        Integer page,
        @Schema(description = "페이지 사이즈", example = "10")
        Integer size,
        @Schema(description = "검색어", example = "맛집")
        String searchWord,
        @Schema(description = "필터 기준 (TITLE, CONTENT, NICK_NAME, ALL)")
        GlobalFilterEnums filter,
        @Schema(description = "상태 필터 (ACTIVE, INACTIVE, ALL) - 관리자용", example = "ACTIVE")
        GlobalActiveEnums active,
        @Schema(description = "게시판 타입 필터 (FREE, SUGGESTION) - 관리자용", example = "FREE")
        PostType searchPostType
) {
    public PostListRequest {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;
        if (filter == null) filter = GlobalFilterEnums.ALL;
        if (searchWord == null) searchWord = "";
        if (active == null) active = GlobalActiveEnums.ACTIVE; // 기본값은 활성 상태만
    }
}