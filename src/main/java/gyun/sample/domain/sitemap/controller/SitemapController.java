package gyun.sample.domain.sitemap.controller;

import gyun.sample.domain.board.enums.PostType;
import gyun.sample.domain.board.repository.PostRepository;
import gyun.sample.global.enums.GlobalActiveEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final PostRepository postRepository;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 1. 메인 페이지
        addUrl(xml, "https://honbob-house.com/", "1.0", LocalDateTime.now());

        // 2. 게시판 목록 페이지
        addUrl(xml, "https://honbob-house.com/board/free/list", "0.8", LocalDateTime.now());
        addUrl(xml, "https://honbob-house.com/board/suggestion/list", "0.8", LocalDateTime.now());

        // 3. 게시글 상세 페이지
        // [수정] 구글 사이트맵 최대 URL 허용량은 50,000개입니다.
        // 기존 3,000개 제한은 서비스 성장 시 이전 글이 누락될 수 있으므로 50,000개로 확장합니다.
        // 필요한 필드만 조회하므로 메모리 부하는 크지 않습니다.
        List<Object[]> postsData = postRepository.findSitemapData(
                GlobalActiveEnums.ACTIVE,
                PageRequest.of(0, 50000)
        );

        for (Object[] row : postsData) {
            Long id = (Long) row[0];
            PostType postType = (PostType) row[1];
            LocalDateTime createdAt = (LocalDateTime) row[2];
            LocalDateTime modifiedAt = (LocalDateTime) row[3];

            if (postType != null) {
                String loc = "https://honbob-house.com/board/" + postType.name().toLowerCase() + "/detail/" + id;
                LocalDateTime lastModTime = modifiedAt != null ? modifiedAt : createdAt;
                addUrl(xml, loc, "0.6", lastModTime);
            }
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    private void addUrl(StringBuilder xml, String loc, String priority, LocalDateTime lastModTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String lastMod = lastModTime.format(formatter);

        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        xml.append("    <lastmod>").append(lastMod).append("</lastmod>\n");
        xml.append("    <changefreq>daily</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("'", "&apos;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}