package gyun.sample.domain.log.service.read;

import gyun.sample.domain.log.entity.PostLog;
import gyun.sample.domain.log.payload.request.PostLogRequest;
import gyun.sample.domain.log.payload.response.PostLogResponse;
import gyun.sample.domain.log.repository.PostLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadPostLogService {

    private final PostLogRepository postLogRepository;

    /**
     * 게시글 활동 로그 목록 조회
     * - 생성일 기준 내림차순 정렬
     * - 검색어(postTitle)가 있으면 필터링
     */
    public Page<PostLogResponse> getPostLogs(PostLogRequest request) {
        // 페이지는 0부터 시작하므로 page - 1 처리
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PostLog> logs;

        // 검색어가 존재하는 경우 제목으로 필터링 조회
        if (request.getSearchWord() != null && !request.getSearchWord().isBlank()) {
            logs = postLogRepository.findByPostTitleContainingIgnoreCase(request.getSearchWord(), pageable);
        } else {
            // 검색어가 없으면 전체 조회
            logs = postLogRepository.findAll(pageable);
        }

        return logs.map(PostLogResponse::new);
    }
}