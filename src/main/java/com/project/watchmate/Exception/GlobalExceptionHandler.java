package com.project.watchmate.Exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MediaNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(MediaNotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(EmailException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(EmailException ex){
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(UsernameException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(UsernameException ex){
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(WatchListNotFoundException.class)
    public ResponseEntity<String> handleWatchlistNotFound(WatchListNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedWatchListAccessException.class)
    public ResponseEntity<String> handleUnauthorizedAccess(UnauthorizedWatchListAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(DuplicateWatchListMediaException.class)
    public ResponseEntity<String> handleDuplicateEntry(DuplicateWatchListMediaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidWatchStatusException.class)
    public ResponseEntity<String> handleInvalidWatchStatus(InvalidWatchStatusException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(MediaNotInWatchListException.class)
    public ResponseEntity<String> handleMediaNotInWatchList(MediaNotInWatchListException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<String> handleReviewNotFound(ReviewNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedReviewAccessException.class)
    public ResponseEntity<String> handleUnauthorizedReviewAccess(UnauthorizedReviewAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Unexpected Error Occurred"));
    }
}
