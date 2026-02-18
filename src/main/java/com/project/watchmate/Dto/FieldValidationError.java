package com.project.watchmate.Dto;

public record FieldValidationError(
    String field,
    String message
) {}
