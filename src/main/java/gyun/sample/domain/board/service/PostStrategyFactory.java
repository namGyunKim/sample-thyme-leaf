package gyun.sample.domain.board.service;

import gyun.sample.domain.board.enums.PostType;
import gyun.sample.domain.board.service.read.AbstractReadPostService;
import gyun.sample.domain.board.service.read.ReadPostService;
import gyun.sample.domain.board.service.write.AbstractWritePostService;
import gyun.sample.domain.board.service.write.WritePostService;
import gyun.sample.global.exception.GlobalException;
import gyun.sample.global.exception.enums.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PostStrategyFactory {

    private final ApplicationContext applicationContext;

    private final Map<PostType, WritePostService> writeServiceMap = new EnumMap<>(PostType.class);
    private final Map<PostType, ReadPostService> readServiceMap = new EnumMap<>(PostType.class);

    private WritePostService defaultWriteService;
    private ReadPostService defaultReadService;


    @PostConstruct
    public void init() {
        initializeWriteServices();
        initializeReadServices();
    }

    private void initializeWriteServices() {
        Map<String, WritePostService> writeServices = applicationContext.getBeansOfType(WritePostService.class);

        for (WritePostService service : writeServices.values()) {
            if (service instanceof AbstractWritePostService abstractService) {
                PostType postType = abstractService.getPostType();
                writeServiceMap.put(postType, service);

                // [수정] 기본 서비스를 FREE로 변경
                if (postType == PostType.FREE) {
                    defaultWriteService = service;
                }
            }
        }
    }

    private void initializeReadServices() {
        Map<String, ReadPostService> readServices = applicationContext.getBeansOfType(ReadPostService.class);

        for (ReadPostService service : readServices.values()) {
            if (service instanceof AbstractReadPostService abstractService) {
                PostType postType = abstractService.getPostType();
                readServiceMap.put(postType, service);

                // [수정] 기본 서비스를 FREE로 변경
                if (postType == PostType.FREE) {
                    defaultReadService = service;
                }
            }
        }
    }


    public WritePostService getWriteService() {
        if (defaultWriteService == null) {
            // MAIN이 없으므로 FREE가 등록되지 않으면 에러가 날 수 있음
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR, "기본 Write 서비스(FREE)가 초기화되지 않았습니다.");
        }
        return defaultWriteService;
    }

    public WritePostService getWriteService(PostType postType) {
        if (postType == null) {
            return getWriteService();
        }

        WritePostService service = writeServiceMap.get(postType);
        if (service == null) {
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR, "지원하지 않는 게시글 타입입니다(Write): " + postType);
        }
        return service;
    }


    public ReadPostService getReadService() {
        if (defaultReadService == null) {
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR, "기본 Read 서비스(FREE)가 초기화되지 않았습니다.");
        }
        return defaultReadService;
    }

    public ReadPostService getReadService(PostType postType) {
        if (postType == null) {
            return getReadService();
        }

        ReadPostService service = readServiceMap.get(postType);
        if (service == null) {
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR, "지원하지 않는 게시글 타입입니다(Read): " + postType);
        }
        return service;
    }
}