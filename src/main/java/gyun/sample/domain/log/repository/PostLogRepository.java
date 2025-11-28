package gyun.sample.domain.log.repository;

import gyun.sample.domain.log.entity.PostLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostLogRepository extends JpaRepository<PostLog, Long> {

    /**
     * 게시글 제목으로 로그 검색 (부분 일치, 대소문자 무시)
     */
    Page<PostLog> findByPostTitleContainingIgnoreCase(String postTitle, Pageable pageable);
}