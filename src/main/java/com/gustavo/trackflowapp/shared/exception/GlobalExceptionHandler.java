package com.gustavo.trackflowapp.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionMessageField> handlerMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        var error = e.getFieldError();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ExceptionMessageField(error.getField(), error.getDefaultMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ExceptionMessage> handlerRuntimeException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ExceptionMessage(e.getMessage()));
    }

    private record ExceptionMessage(String error) {
    }

    private record ExceptionMessageField(String field, String message) {
        public ExceptionMessageField(FieldError fieldError) {
            this(fieldError.getField(), fieldError.getDefaultMessage());
        }
    }
}
