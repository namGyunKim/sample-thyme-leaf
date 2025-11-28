package gyun.sample.domain.board.validator;

import gyun.sample.domain.board.payload.request.PostCreateRequest;
import gyun.sample.domain.board.payload.request.PostUpdateRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class PostValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return PostCreateRequest.class.isAssignableFrom(clazz) || PostUpdateRequest.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        // 공통 검증 로직
        if (target instanceof PostCreateRequest request) {
            if (!StringUtils.hasText(request.title())) {
                errors.rejectValue("title", "title.empty", "제목을 입력해주세요.");
            }
            if (!StringUtils.hasText(request.content())) {
                errors.rejectValue("content", "content.empty", "내용을 입력해주세요.");
            }
        }
        // Update 검증도 유사하게 가능
    }
}