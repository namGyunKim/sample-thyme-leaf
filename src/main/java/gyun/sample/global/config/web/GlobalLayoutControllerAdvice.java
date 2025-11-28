package gyun.sample.global.config.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 전역 레이아웃 데이터 공급자
 * - Thymeleaf 3.1+ 에서는 #request 객체 직접 접근이 불가능하므로,
 * 공통적으로 필요한 URL 정보 등을 이곳에서 모델에 담아 뷰로 전달합니다.
 */
@ControllerAdvice
public class GlobalLayoutControllerAdvice {

    @ModelAttribute("currentUrl")
    public String getCurrentUrl(HttpServletRequest request) {
        return request.getRequestURL().toString();
    }

    @ModelAttribute("requestURI")
    public String getRequestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }
}