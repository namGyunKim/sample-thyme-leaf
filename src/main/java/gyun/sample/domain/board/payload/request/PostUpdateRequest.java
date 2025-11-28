package gyun.sample.domain.board.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record PostUpdateRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        @Schema(description = "수정할 제목", example = "수정된 제목")
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        @Schema(description = "수정할 내용", example = "수정된 내용")
        String content
) {
}