package gyun.sample.domain.board.payload.response;

import gyun.sample.domain.board.entity.Post;
import gyun.sample.global.enums.GlobalActiveEnums;
import gyun.sample.global.utils.UtilService;

public record PostListResponse(
        Long id,
        String title,
        String authorNickname,
        int viewCount,
        int likeCount,
        boolean isPinned,
        GlobalActiveEnums active, // 활성 상태 필드
        String postType,          // [수정] 필드 추가: 게시글 타입 (타임리프에서 접근 필요)
        String thumbnailUrl,
        boolean hasImage,
        String createdAt
) {
    public PostListResponse(Post post) {
        this(
                post.getId(),
                post.getTitle(),
                post.getAuthor().getNickName(),
                post.getViewCount(),
                post.getLikeCount(),
                post.isPinned(),
                post.getActive(),
                // [수정] Post Entity에서 타입 가져와서 매핑 (null일 경우 기본값 처리)
                post.getPostType() != null ? post.getPostType().name() : "FREE",
                extractThumbnail(post.getContent()),
                post.getContent() != null && post.getContent().contains("<img"),
                UtilService.formattedTime(post.getCreatedAt())
        );
    }

    private static String extractThumbnail(String content) {
        if (content == null || !content.contains("<img")) return null;
        try {
            int srcStart = content.indexOf("src=\"");
            if (srcStart == -1) return null;
            int srcEnd = content.indexOf("\"", srcStart + 5);
            if (srcEnd == -1) return null;
            return content.substring(srcStart + 5, srcEnd);
        } catch (Exception e) {
            return null;
        }
    }
}