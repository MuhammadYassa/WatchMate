package com.project.watchmate.Models;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table
public class FollowRequest {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Users requestUser;

    @ManyToOne
    private Users targetUser;

    private LocalDateTime requestedAt;

    private LocalDateTime respondedAt;

    @Enumerated(EnumType.STRING)
    private FollowRequestStatuses status;
}
