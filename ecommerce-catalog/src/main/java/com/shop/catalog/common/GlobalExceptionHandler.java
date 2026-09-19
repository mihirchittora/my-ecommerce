package com.shop.catalog.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> conflict(ConflictException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ApiError> badRequest(BadRequestException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> constraint(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", details);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> methodValidation(HandlerMethodValidationException ex) {
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST,
                "Invalid value for request parameter: " + ex.getName(), List.of());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, HttpMediaTypeNotSupportedException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> requestFormat(Exception ex) {
        String message = ex.getMessage() == null ? "Invalid request format" : ex.getMessage();
        return error(HttpStatus.BAD_REQUEST, "Invalid request format", List.of(message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> uploadTooLarge(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.BAD_REQUEST, "Uploaded file is too large", List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> dataIntegrity(DataIntegrityViolationException ex) {
        String databaseMessage = rootMessage(ex).toLowerCase(java.util.Locale.ROOT);
        if (databaseMessage.contains("product_variants") && databaseMessage.contains("sku")) {
            Matcher matcher = Pattern.compile("key \\(sku\\)=\\(([^)]*)\\)", Pattern.CASE_INSENSITIVE)
                    .matcher(rootMessage(ex));
            String sku = matcher.find() ? matcher.group(1) : null;
            return error(HttpStatus.CONFLICT,
                    sku == null || sku.isBlank() ? "SKU already exists" : "SKU already exists: " + sku,
                    List.of());
        }
        return error(HttpStatus.CONFLICT, "Database constraint violation", List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> generic(Exception ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", List.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, List<String> details) {
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                details);
        return ResponseEntity.status(status).body(body);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "" : current.getMessage();
    }
}
