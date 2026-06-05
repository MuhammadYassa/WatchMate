package com.project.watchmate.media.catalog.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@Table(
    name = "genre",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_genre_tmdb_genre_type", columnNames = {"tmdb_genre_id", "media_type"}),
        @UniqueConstraint(name = "uq_genre_name_type", columnNames = {"name", "media_type"})
    }
)
@NoArgsConstructor
@AllArgsConstructor
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_genre_id", nullable = false)
    private Long tmdbGenreId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @Column(nullable = false)
    private LocalDateTime syncedAt;

    @Builder.Default
    @ManyToMany(mappedBy = "genres")
    private List<Media> media = new ArrayList<>();
}

