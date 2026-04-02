package com.project.watchmate.Dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiError", description = "Standard error response returned by the API.")
public record ApiError(
    @Schema(description = "Human-readable error message.", example = "Error Message")
    String message,
    @Schema(description = "Stable application error code.", example = "ERROR_CODE")
    String code,
    @Schema(description = "Field-level validation details when available.")
    List<FieldValidationError> fields
) {}
