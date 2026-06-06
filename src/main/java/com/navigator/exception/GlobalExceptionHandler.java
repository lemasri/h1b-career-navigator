package com.navigator.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralised error handling for bean-validation failures.
 *
 * Spring's default handling exposes each {@link FieldError}'s rejected value —
 * which for an auth request is the plaintext the user submitted (e.g. a too-short
 * password). This advice builds the response from field name + message only, so
 * sensitive input never reaches the response body or the logs.
 *
 * Exceptions annotated with {@code @ResponseStatus} (ResourceNotFoundException,
 * EmailAlreadyExistsException, InvalidCredentialsException) are intentionally not
 * handled here — they keep their existing status mapping.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            // Field name + message only — never error.getRejectedValue().
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        // Log field names only, not the messages' subjects or any values.
        log.warn("Validation failed for {}: invalid fields {}",
                ex.getObjectName(), fieldErrors.keySet());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation failed");
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }
}
