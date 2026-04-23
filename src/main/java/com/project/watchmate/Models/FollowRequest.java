package com.project.watchmate.Models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "request_user_id", nullable = false)
    private Users requestUser;

    @ManyToOne
    @JoinColumn(name = "target_user_id", nullable = false)
    private Users targetUser;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime respondedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FollowRequestStatuses status;
}
