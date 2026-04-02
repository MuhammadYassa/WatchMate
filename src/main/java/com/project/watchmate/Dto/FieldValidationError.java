package com.project.watchmate.Dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FieldValidationError", description = "Validation detail for a single request field.")
public record FieldValidationError(
    @Schema(description = "Field name that failed validation.", example = "example_field")
    String field,
    @Schema(description = "Validation message for the field.", example = "field validation error message")
    String message
) {}
