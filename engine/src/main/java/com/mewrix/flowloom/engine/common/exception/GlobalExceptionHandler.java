package com.mewrix.flowloom.engine.common.exception;

import com.mewrix.flowloom.engine.common.logging.MdcKeys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Spring MVC 全局异常处理器
 *
 * <p>
 * 统一处理 Spring MVC 请求异常及未被专用异常处理器处理的异常，
 * 并返回规范化的错误响应。
 * </p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * 处理 Spring MVC 请求处理过程中未被其他异常处理器处理的异常。
     *
     * <p>
     * 记录异常堆栈及请求信息，并返回统一的 HTTP 500 错误响应，
     * 避免向客户端暴露内部异常信息。
     * </p>
     *
     * @param ex      未处理的异常
     * @param request 当前 HTTP 请求
     * @return 包含错误详情的 HTTP 500 响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        String requestId = MDC.get(MdcKeys.REQUEST_ID);

        log.error(
                "Unexpected exception: method={}, path={}, requestId={}",
                request.getMethod(),
                request.getRequestURI(),
                requestId,
                ex
        );

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred."
        );

        problem.setTitle("Internal Server Error");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problem);
    }
}
