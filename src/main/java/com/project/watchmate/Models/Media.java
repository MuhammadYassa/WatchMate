package com.project.watchmate.Models;

import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table
@Builder
public class Media {
    
    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "tmdb_id", unique = true, nullable = false)
    private Long tmdbId;

    private String title;

    @Lob
    private String overview;

    private String posterPath;

    private LocalDate releaseDate;

    @Enumerated(EnumType.STRING)
    private MediaType type;
    
    private Double rating;

    @Builder.Default
    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserMediaStatus> userStatuses = new ArrayList<>();

    @Builder.Default
    @ManyToMany(mappedBy = "media")
    private List<WatchList> watchLists = new ArrayList<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
        name = "media_genres",
        joinColumns = @JoinColumn(name = "media_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private List<Genre> genres = new ArrayList<>();

    @OneToMany(mappedBy = "media")
    private List<Review> reviews;
}
