package com.project.watchmate.Exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.project.watchmate.Dto.ApiError;
import com.project.watchmate.Dto.FieldValidationError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import software.amazon.awssdk.services.ses.model.SesException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MediaNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(MediaNotFoundException ex, HttpServletRequest request){
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "MEDIA_NOT_FOUND", null, ex, request);
    }

    @ExceptionHandler(EmailException.class)
    public ResponseEntity<ApiError> handleDuplicate(EmailException ex, HttpServletRequest request){
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), "DUPLICATE_EMAIL", null, ex, request);
    }

    @ExceptionHandler(UsernameException.class)
    public ResponseEntity<ApiError> handleDuplicate(UsernameException ex, HttpServletRequest request){
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), "DUPLICATE_USERNAME", null, ex, request);
    }

    @ExceptionHandler(SesException.class)
    public ResponseEntity<ApiError> handleSesException(SesException ex, HttpServletRequest request){
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "SES_EXCEPTION", null, ex, request);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiError> handleInvalidRefreshToken(InvalidRefreshTokenException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), "INVALID_REFRESH_TOKEN", null, ex, request);
    }

    @ExceptionHandler(WatchListNotFoundException.class)
    public ResponseEntity<ApiError> handleWatchlistNotFound(WatchListNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "WATCHLIST_NOT_FOUND", null, ex, request);
    }

    @ExceptionHandler(UnauthorizedWatchListAccessException.class)
    public ResponseEntity<ApiError> handleUnauthorizedAccess(UnauthorizedWatchListAccessException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), "UNAUTHORIZED_WATCHLIST_ACCESS", null, ex, request);
    }

    @ExceptionHandler(DuplicateWatchListMediaException.class)
    public ResponseEntity<ApiError> handleDuplicateEntry(DuplicateWatchListMediaException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), "DUPLICATE_WATCHLIST_ENTRY", null, ex, request);
    }

    @ExceptionHandler(InvalidWatchStatusException.class)
    public ResponseEntity<ApiError> handleInvalidWatchStatus(InvalidWatchStatusException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_WATCH_STATUS", null, ex, request);
    }

    @ExceptionHandler(MediaNotInWatchListException.class)
    public ResponseEntity<ApiError> handleMediaNotInWatchList(MediaNotInWatchListException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "MEDIA_NOT_IN_WATCHLIST", null, ex, request);
    }

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<ApiError> handleReviewNotFound(ReviewNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "REVIEW_NOT_FOUND", null, ex, request);
    }

    @ExceptionHandler(UnauthorizedReviewAccessException.class)
    public ResponseEntity<ApiError> handleUnauthorizedReviewAccess(UnauthorizedReviewAccessException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), "UNAUTHORIZED_REVIEW_ACCESS", null, ex, request);
    }

    @ExceptionHandler(DuplicateFavouriteException.class)
    public ResponseEntity<ApiError> handleDuplicateFavourite(DuplicateFavouriteException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), "DUPLICATE_FAVOURITE", null, ex, request);
    }

    @ExceptionHandler(DuplicateReviewException.class)
    public ResponseEntity<ApiError> handleDuplicateReview(DuplicateReviewException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), "DUPLICATE_REVIEW", null, ex, request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "USER_NOT_FOUND", null, ex, request);
    }

    @ExceptionHandler(SelfFollowException.class)
    public ResponseEntity<ApiError> handleSelfFollow(SelfFollowException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "SELF_FOLLOW_ERROR", null, ex, request);
    }

    @ExceptionHandler(AlreadyFollowingException.class)
    public ResponseEntity<ApiError> handleAlreadyFollowing(AlreadyFollowingException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), "ALREADY_FOLLOWING_ERROR", null, ex, request);
    }

    @ExceptionHandler(NotFollowingException.class)
    public ResponseEntity<ApiError> handleNotFollowing(NotFollowingException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "NOT_FOLLOWING_ERROR", null, ex, request);
    }

    @ExceptionHandler(UnauthorizedFollowRequestAccessException.class)
    public ResponseEntity<ApiError> handleUnauthorizedFollowRequestAccess(UnauthorizedFollowRequestAccessException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), "UNAUTHORIZED_FOLLOW_REQUEST_ACCESS", null, ex, request);
    }

    @ExceptionHandler(FollowRequestNotFoundException.class)
    public ResponseEntity<ApiError> handleFollowRequestNotFound(FollowRequestNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "FOLLOW_REQUEST_NOT_FOUND", null, ex, request);
    }

    @ExceptionHandler(BlockedUserException.class)
    public ResponseEntity<ApiError> handleBlocked(BlockedUserException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), "USER_BLOCKED", null, ex, request);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldValidationError> fields =
            ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldValidationError(e.getField(), e.getDefaultMessage()))
                .toList();

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", "VALIDATION_ERROR", fields, ex, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<FieldValidationError> fields = ex.getConstraintViolations().stream()
            .map(v -> new FieldValidationError(
                v.getPropertyPath() == null ? null : v.getPropertyPath().toString(),
                v.getMessage()
            ))
            .toList();

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", "VALIDATION_ERROR", fields, ex, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String field = ex.getName();
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "Invalid value for parameter: " + field,
            "TYPE_MISMATCH",
            List.of(new FieldValidationError(field, "Invalid value")),
            ex,
            request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "BAD_REQUEST", null, ex, request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), "CONFLICT", null, ex, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Authentication failed", "AUTH_FAILED", null, ex, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request){
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Error Occured", "UNEXPECTED_ERROR", null, ex, request);
    }

    private ResponseEntity<ApiError> buildResponse(HttpStatus status, String message, String code,
            List<FieldValidationError> fields, Exception ex, HttpServletRequest request) {
        if (status.is5xxServerError()) {
            log.error("Request failed method={} path={} status={} code={}", request.getMethod(), request.getRequestURI(), status.value(), code, ex);
        } else {
            log.warn("Request handled with error method={} path={} status={} code={} reason={}",
                request.getMethod(),
                request.getRequestURI(),
                status.value(),
                code,
                ex.getClass().getSimpleName());
        }

        return ResponseEntity.status(status).body(new ApiError(message, code, fields));
    }
}
