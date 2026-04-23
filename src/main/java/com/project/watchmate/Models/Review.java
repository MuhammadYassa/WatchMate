package com.project.watchmate.Models;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(uniqueConstraints = @UniqueConstraint(name = "uq_review_user_media", columnNames = {"user_id", "media_id"}))
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @Min(1)
    @Max(5)
    private int rating;

    private String comment;

    private LocalDateTime datePosted;

    private LocalDateTime dateLastModified;
    
}
