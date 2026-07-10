package me.choir_backend.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;


@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({
            WrongSessionTypeException.class,
            MemberAlreadyCheckedInException.class,
            InsufficientTicketsException.class
    })
    public ResponseEntity<String> handleBadRequestException(RuntimeException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        log.error("Unhandled exception [{}]", errorId, ex);
        return new ResponseEntity<>("An unexpected error occurred (Fehler-ID: " + errorId + ")",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
