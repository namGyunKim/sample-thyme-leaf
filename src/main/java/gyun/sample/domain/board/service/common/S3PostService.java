package gyun.sample.domain.board.service.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3PostService {
    // 게시글 내 이미지 처리 로직 (S3Service 활용)
    // 현재는 에디터 이미지 업로드 처리 등을 위한 뼈대만 유지
    // 추후 Summernote나 Toast UI Editor 이미지 업로드 시 사용
}