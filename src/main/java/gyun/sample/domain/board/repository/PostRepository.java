package gyun.sample.domain.board.repository;

import gyun.sample.domain.board.entity.Post;
import gyun.sample.global.enums.GlobalActiveEnums;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, QuerydslPredicateExecutor<Post>, PostRepositoryCustom {

    // [수정] 기존 엔티티 전체 조회 대신, 사이트맵에 필요한 필드만 조회하는 쿼리 추가 (성능 최적화)
    // Content(본문) 같은 무거운 데이터를 제외하여 메모리 사용량을 획기적으로 줄입니다.
    // 반환 타입 Object[]: [0]=id, [1]=postType, [2]=createdAt, [3]=modifiedAt
    @Query("SELECT p.id, p.postType, p.createdAt, p.modifiedAt FROM Post p WHERE p.active = :active ORDER BY p.createdAt DESC")
    List<Object[]> findSitemapData(@Param("active") GlobalActiveEnums active, Pageable pageable);

    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void increaseViewCount(Long postId);

    // 기존 findByActive는 다른 로직에서 쓰지 않는다면 삭제하거나 유지해도 됩니다.
    // 여기서는 호환성을 위해 남겨두되, SitemapController에서는 위 메서드를 쓸 것입니다.
    List<Post> findByActive(GlobalActiveEnums active, Pageable pageable);
}