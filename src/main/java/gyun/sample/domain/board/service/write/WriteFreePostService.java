package gyun.sample.domain.board.service.write;

import gyun.sample.domain.board.enums.PostType;
import gyun.sample.domain.board.repository.PostLikeRepository;
import gyun.sample.domain.board.repository.PostRepository;
import gyun.sample.domain.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class WriteFreePostService extends AbstractWritePostService {

    public WriteFreePostService(PostRepository postRepository,
                                MemberRepository memberRepository,
                                PostLikeRepository postLikeRepository,
                                ApplicationEventPublisher eventPublisher,
                                HttpServletRequest httpServletRequest) {
        super(postRepository, memberRepository, postLikeRepository, eventPublisher, httpServletRequest);
    }

    @Override
    public PostType getPostType() {
        return PostType.FREE;
    }
}