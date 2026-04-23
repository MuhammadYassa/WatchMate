package com.project.watchmate.Models;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uq_user_media_status_user_media", columnNames = {"user_id", "media_id"}))
@Builder
public class UserMediaStatus {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WatchStatus status;

}
