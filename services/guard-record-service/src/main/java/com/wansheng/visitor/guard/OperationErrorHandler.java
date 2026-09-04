package com.wansheng.visitor.guard;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
class OperationErrorHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ProblemDetail> handle(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(ProblemDetail.forStatusAndDetail(
                exception.getStatusCode(), exception.getReason()==null ? "请求无法完成，请刷新后重试" : exception.getReason()));
    }
}
