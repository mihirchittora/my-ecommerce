package com.shop.customer.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ApiError api(ApiException ex, HttpServletRequest request) {
        return new ApiError(ex.getStatus().value(), ex.getCode(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ApiError validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return new ApiError(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", "Request validation failed", request.getRequestURI(), fields);
    }

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
    ApiError constraint(Exception ex, HttpServletRequest request) {
        return new ApiError(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", "Request validation failed", request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ApiError unreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return new ApiError(HttpStatus.BAD_REQUEST.value(), "INVALID_REQUEST", "Request body is invalid or contains unsupported fields", request.getRequestURI());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingPathVariableException.class,
            MissingServletRequestParameterException.class})
    ApiError requestParameter(Exception ex, HttpServletRequest request) {
        return new ApiError(HttpStatus.BAD_REQUEST.value(), "INVALID_REQUEST", "A path or query parameter is invalid or missing", request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ApiError conflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        return new ApiError(HttpStatus.CONFLICT.value(), "CONFLICT", "The requested customer data conflicts with an existing record", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    ApiError unexpected(Exception ex, HttpServletRequest request) {
        return new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR", "An unexpected error occurred", request.getRequestURI());
    }
}
