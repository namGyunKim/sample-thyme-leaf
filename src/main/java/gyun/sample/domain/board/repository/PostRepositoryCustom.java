package gyun.sample.domain.board.repository;

import com.querydsl.core.types.Predicate;
import gyun.sample.domain.board.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepositoryCustom {
    /**
     * [성능 개선] 게시글 목록 조회 시 작성자 정보를 Fetch Join으로 함께 가져옵니다.
     * 이를 통해 N+1 문제를 방지합니다.
     */
    Page<Post> findAllWithAuthor(Predicate predicate, Pageable pageable);
}