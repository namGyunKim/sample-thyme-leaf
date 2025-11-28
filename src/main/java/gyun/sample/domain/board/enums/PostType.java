package gyun.sample.domain.board.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum PostType {

    FREE("FREE", "자유게시판", "자유롭게 이야기를 나누는 공간입니다."),
    SUGGESTION("SUGGESTION", "건의게시판", "서비스 개선을 위한 소중한 의견을 남겨주세요.");

    private final String code;
    private final String title;       // 게시판 이름 (ex: 자유게시판)
    private final String description; // 게시판 설명 (ex: 자유롭게...)

    @JsonCreator
    public static PostType create(String requestValue) {
        if (requestValue == null) {
            return null;
        }
        return Stream.of(values())
                .filter(v -> v.name().equalsIgnoreCase(requestValue)) // 대소문자 무시 검색
                .findFirst()
                .orElse(null);
    }
}