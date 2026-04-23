package com.project.watchmate.Models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uq_email_verification_token_user", columnNames = "user_id"))
@Builder
public class EmailVerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;
}
