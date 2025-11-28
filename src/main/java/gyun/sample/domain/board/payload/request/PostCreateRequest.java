package gyun.sample.domain.board.payload.request;

import gyun.sample.domain.board.enums.PostType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PostCreateRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        @Schema(description = "게시글 제목", example = "혼밥하기 좋은 식당 추천")
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        @Schema(description = "게시글 내용", example = "강남역 근처...")
        String content,

        @NotNull(message = "게시판 타입을 선택해주세요.")
        @Schema(description = "게시판 타입", example = "FREE")
        PostType postType
) {
}