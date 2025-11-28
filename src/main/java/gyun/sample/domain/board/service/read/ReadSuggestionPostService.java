package gyun.sample.domain.board.service.read;

import gyun.sample.domain.board.enums.PostType;
import gyun.sample.domain.board.repository.PostLikeRepository;
import gyun.sample.domain.board.repository.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class ReadSuggestionPostService extends AbstractReadPostService {

    public ReadSuggestionPostService(PostRepository postRepository, PostLikeRepository postLikeRepository) {
        super(postRepository, postLikeRepository);
    }

    @Override
    public PostType getPostType() {
        return PostType.SUGGESTION;
    }
}