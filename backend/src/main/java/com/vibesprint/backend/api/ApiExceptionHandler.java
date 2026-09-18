package com.vibesprint.backend.api;

import com.vibesprint.backend.progression.DemoStateNotReadyException;
import com.vibesprint.backend.progression.InvalidProgressionCommandException;
import com.vibesprint.backend.progression.QuestNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({
            InvalidProgressionCommandException.class,
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiErrorResponse> invalidRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request is invalid");
    }

    @ExceptionHandler(QuestNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> questNotFound(QuestNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "QUEST_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(DemoStateNotReadyException.class)
    public ResponseEntity<ApiErrorResponse> demoStateNotReady(DemoStateNotReadyException exception) {
        return error(HttpStatus.CONFLICT, "DEMO_STATE_NOT_READY", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> internalError(Exception exception) {
        LOGGER.error("Unexpected API error", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error");
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, message));
    }
}
