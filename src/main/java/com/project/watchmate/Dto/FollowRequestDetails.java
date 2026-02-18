package com.project.watchmate.Dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowRequestDetails {

    @NotBlank
    private String requesterUsername;

    @NotNull
    private Long requesterUserId;

    @NotNull
    private LocalDateTime requestedAt;

}
