package gyun.sample.domain.log.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LogType {
    JOIN("회원가입"),
    LOGIN("로그인"),
    LOGIN_FAIL("로그인 실패"),
    UPDATE("정보수정"),
    INACTIVE("탈퇴/비활성화"),
    PASSWORD_CHANGE("비밀번호 변경"),

    // 게시글 관련 로그 타입 추가
    POST_CREATE("게시글 작성"),
    POST_UPDATE("게시글 수정"),
    POST_DELETE("게시글 삭제"),
    POST_VIEW("게시글 조회");

    private final String description;
}