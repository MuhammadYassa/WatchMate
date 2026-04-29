package com.project.watchmate.Models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "genre_lookup",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_genre_lookup_tmdb_genre_type", columnNames = {"tmdb_genre_id", "media_type"}),
        @UniqueConstraint(name = "uq_genre_lookup_name_type", columnNames = {"name", "media_type"})
    }
)
public class GenreLookup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_genre_id", nullable = false)
    private Long tmdbGenreId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType;

    @Column(nullable = false)
    private LocalDateTime syncedAt;
}
