package com.supermarket.inventory.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
  ResponseEntity<ApiResponse<Void>> lockConflict(Exception e) {
    return ResponseEntity.status(409)
        .body(ApiResponse.error("RETRYABLE_CONFLICT", "当前业务发生并发冲突，已回滚，请刷新单据后重试"));
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ApiResponse<Void>> methodNotAllowed(Exception e) {
    return ResponseEntity.status(405).body(ApiResponse.error("METHOD_NOT_ALLOWED", "此接口不支持该操作"));
  }

  @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
  ResponseEntity<ApiResponse<Void>> missingResource(Exception e) {
    return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "接口或资源不存在"));
  }

  @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
  ResponseEntity<ApiResponse<Void>> tooLarge(Exception e) {
    return ResponseEntity.badRequest().body(ApiResponse.error("IMAGE_INVALID", "图片不得超过2 MB"));
  }

  @ExceptionHandler(BusinessException.class)
  ResponseEntity<ApiResponse<Void>> business(BusinessException e) {
    return ResponseEntity.status(e.status()).body(ApiResponse.error(e.code(), e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + "：" + error.getDefaultMessage())
            .distinct()
            .reduce((a, b) -> a + "；" + b)
            .orElse("输入不合法");
    return ResponseEntity.badRequest().body(ApiResponse.error("VALIDATION_ERROR", message));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    org.springframework.web.multipart.support.MissingServletRequestPartException.class,
    ConstraintViolationException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<ApiResponse<Void>> malformed(Exception e) {
    return ResponseEntity.badRequest()
        .body(ApiResponse.error("VALIDATION_ERROR", "字段类型、格式或请求字段不合法"));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiResponse<Void>> integrity(DataIntegrityViolationException e) {
    return ResponseEntity.status(409)
        .body(ApiResponse.error("DATA_CONFLICT", "编码、条码或名称重复，或记录已被业务引用，请检查后重试"));
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ApiResponse<Void>> denied(AccessDeniedException e) {
    return ResponseEntity.status(403).body(ApiResponse.error("FORBIDDEN", "无权执行此操作"));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiResponse<Void>> unexpected(Exception e) {
    LOG.error("Request failed; traceId identifies the request", e);
    return ResponseEntity.internalServerError()
        .body(ApiResponse.error("INTERNAL_ERROR", "服务暂时异常，请凭追踪编号联系管理员"));
  }
}
