package com.project.watchmate.Dto;

import java.util.List;

public record ApiError(
    String message,
    String code,
    List<FieldValidationError> fields
) {}
