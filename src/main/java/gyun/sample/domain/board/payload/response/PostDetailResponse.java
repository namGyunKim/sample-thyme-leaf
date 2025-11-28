package gyun.sample.domain.board.payload.response;

import gyun.sample.domain.board.entity.Post;
import gyun.sample.global.utils.UtilService;

public record PostDetailResponse(
        Long postId,
        String title,
        String content,
        String authorNickname,
        Long authorUserId,
        int viewCount,
        int likeCount,
        String postType,
        boolean isLiked,
        String createdAt,
        // [SEO] 검색엔진용 ISO 8601 날짜 포맷 (yyyy-MM-dd'T'HH:mm:ss)
        String createdAtIso,
        // [SEO] 게시글 대표 썸네일 URL (없으면 null)
        String thumbnailUrl
) {
    public PostDetailResponse(Post post, Long currentUserId) {
        this(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getNickName(),
                post.getAuthor().getId(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getPostType().name(),
                false, // Controller에서 처리됨
                UtilService.formattedTime(post.getCreatedAt()),
                // [SEO] ISO 포맷 변환
                post.getCreatedAt() != null ? post.getCreatedAt().toString() : "",
                // [SEO] 본문에서 첫 번째 이미지 URL 추출
                extractThumbnail(post.getContent())
        );
    }

    // 본문에서 첫 번째 이미지 태그의 src 추출 로직
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