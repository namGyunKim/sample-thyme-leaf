package gyun.sample.domain.board.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import gyun.sample.domain.board.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.ArrayList;
import java.util.List;

import static gyun.sample.domain.board.entity.QPost.post;
import static gyun.sample.domain.member.entity.QMember.member;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Post> findAllWithAuthor(Predicate predicate, Pageable pageable) {
        List<Post> content = queryFactory
                .selectFrom(post)
                .leftJoin(post.author, member).fetchJoin() // [핵심] Fetch Join 적용
                .where(predicate)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(getOrderSpecifiers(pageable.getSort()))
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(post.count())
                .from(post)
                .where(predicate);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // Sort 객체를 QueryDSL OrderSpecifier로 변환
    private OrderSpecifier<?>[] getOrderSpecifiers(Sort sort) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        sort.forEach(order -> {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            String prop = order.getProperty();

            if (prop.equals("createdAt")) {
                orders.add(new OrderSpecifier<>(direction, post.createdAt));
            } else if (prop.equals("isPinned")) {
                orders.add(new OrderSpecifier<>(direction, post.isPinned));
            }
            // 필요시 다른 정렬 조건 추가
        });
        return orders.toArray(new OrderSpecifier[0]);
    }
}