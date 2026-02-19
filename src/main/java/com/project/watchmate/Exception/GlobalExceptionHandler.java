package com.project.watchmate.Exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.project.watchmate.Dto.ApiError;
import com.project.watchmate.Dto.FieldValidationError;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MediaNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(MediaNotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(ex.getMessage(), "MEDIA_NOT_FOUND", null));
    }

    @ExceptionHandler(EmailException.class)
    public ResponseEntity<ApiError> handleDuplicate(EmailException ex){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(ex.getMessage(), "DUPLICATE_EMAIL", null));
    }

    @ExceptionHandler(UsernameException.class)
    public ResponseEntity<ApiError> handleDuplicate(UsernameException ex){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(ex.getMessage(), "DUPLICATE_USERNAME", null));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiError> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError(ex.getMessage(), "INVALID_REFRESH_TOKEN", null));
    }

    @ExceptionHandler(WatchListNotFoundException.class)
    public ResponseEntity<ApiError> handleWatchlistNotFound(WatchListNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(ex.getMessage(), "WATCHLIST_NOT_FOUND", null));
    }

    @ExceptionHandler(UnauthorizedWatchListAccessException.class)
    public ResponseEntity<ApiError> handleUnauthorizedAccess(UnauthorizedWatchListAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(ex.getMessage(), "UNAUTHORIZED_WATCHLIST_ACCESS", null));
    }

    @ExceptionHandler(DuplicateWatchListMediaException.class)
    public ResponseEntity<ApiError> handleDuplicateEntry(DuplicateWatchListMediaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(ex.getMessage(), "DUPLICATE_WATCHLIST_ENTRY", null));
    }

    @ExceptionHandler(InvalidWatchStatusException.class)
    public ResponseEntity<ApiError> handleInvalidWatchStatus(InvalidWatchStatusException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(ex.getMessage(), "INVALID_WATCH_STATUS", null));
    }

    @ExceptionHandler(MediaNotInWatchListException.class)
    public ResponseEntity<ApiError> handleMediaNotInWatchList(MediaNotInWatchListException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(ex.getMessage(), "MEDIA_NOT_IN_WATCHLIST", null));
    }

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<ApiError> handleReviewNotFound(ReviewNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(ex.getMessage(), "REVIEW_NOT_FOUND", null));
    }

    @ExceptionHandler(UnauthorizedReviewAccessException.class)
    public ResponseEntity<ApiError> handleUnauthorizedReviewAccess(UnauthorizedReviewAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(ex.getMessage(), "UNAUTHORIZED_REVIEW_ACCESS", null));
    }

    @ExceptionHandler(DuplicateFavouriteException.class)
    public ResponseEntity<ApiError> handleDuplicateFavourite(DuplicateFavouriteException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(ex.getMessage(), "DUPLICATE_FAVOURITE", null));
    }

    @ExceptionHandler(DuplicateReviewException.class)
    public ResponseEntity<ApiError> handleDuplicateReview(DuplicateReviewException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(ex.getMessage(), "DUPLICATE_REVIEW", null));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(ex.getMessage(), "USER_NOT_FOUND", null));
    }

    @ExceptionHandler(SelfFollowException.class)
    public ResponseEntity<ApiError> handleSelfFollow(SelfFollowException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(ex.getMessage(), "SELF_FOLLOW_ERROR", null));
    }

    @ExceptionHandler(AlreadyFollowingException.class)
    public ResponseEntity<ApiError> handleAlreadyFollowing(AlreadyFollowingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(ex.getMessage(), "ALREADY_FOLLOWING_ERROR", null));
    }

    @ExceptionHandler(NotFollowingException.class)
    public ResponseEntity<ApiError> handleNotFollowing(NotFollowingException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(ex.getMessage(), "NOT_FOLLOWING_ERROR", null));
    }

    @ExceptionHandler(UnauthorizedFollowRequestAccessException.class)
    public ResponseEntity<ApiError> handleUnauthorizedFollowRequestAccess(UnauthorizedFollowRequestAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(ex.getMessage(), "UNAUTHORIZED_FOLLOW_REQUEST_ACCESS", null));
    }

    @ExceptionHandler(FollowRequestNotFoundException.class)
    public ResponseEntity<ApiError> handleFollowRequestNotFound(FollowRequestNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(ex.getMessage(), "FOLLOW_REQUEST_NOT_FOUND", null));
    }

    @ExceptionHandler(BlockedUserException.class)
    public ResponseEntity<ApiError> handleBlocked(BlockedUserException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(ex.getMessage(), "USER_BLOCKED", null));
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldValidationError> fields =
            ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldValidationError(e.getField(), e.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest()
            .body(new ApiError("Validation failed", "VALIDATION_ERROR", fields));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldValidationError> fields = ex.getConstraintViolations().stream()
            .map(v -> new FieldValidationError(
                v.getPropertyPath() == null ? null : v.getPropertyPath().toString(),
                v.getMessage()
            ))
            .toList();

        return ResponseEntity.badRequest()
            .body(new ApiError("Validation failed", "VALIDATION_ERROR", fields));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String field = ex.getName();
        return ResponseEntity.badRequest()
            .body(new ApiError("Invalid value for parameter: " + field, "TYPE_MISMATCH", List.of(new FieldValidationError(field, "Invalid value"))));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ApiError(ex.getMessage(), "BAD_REQUEST", null));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(ex.getMessage(), "CONFLICT", null));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError("Authentication failed", "AUTH_FAILED", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError("Unexpected Error Occured", "UNEXPECTED_ERROR", null));
    }
}
