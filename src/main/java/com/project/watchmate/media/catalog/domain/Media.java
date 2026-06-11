package com.project.watchmate.media.catalog.domain;

import com.project.watchmate.review.domain.Review;
import com.project.watchmate.movie.tracking.domain.UserMediaStatus;
import com.project.watchmate.watchlist.domain.WatchListItem;

import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uq_media_tmdb_id_type", columnNames = {"tmdb_id", "type"}))
@Builder
public class Media {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_id", nullable = false)
    private Long tmdbId;

    private String title;

    @Lob
    private String overview;

    private String posterPath;

    private String backdropPath;

    private LocalDate releaseDate;

    private LocalDate nextEpisodeAirDate;

    private Integer nextEpisodeSeasonNumber;

    private Integer nextEpisodeEpisodeNumber;

    private String nextEpisodeName;

    private Integer lastEpisodeToAirSeasonNumber;

    private Integer lastEpisodeToAirEpisodeNumber;

    private String lastEpisodeToAirName;
    
    private LocalDate lastAirDate;

    private Integer numberOfSeasons;

    private Integer numberOfEpisodes;

    private String tmdbShowStatus;

    private LocalDateTime nextAiringSyncedAt;

    private LocalDateTime lastTmdbSyncAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MediaType type;
    
    private Double rating;

    @Builder.Default
    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserMediaStatus> userStatuses = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WatchListItem> watchListItems = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShowSeason> showSeasons = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShowEpisode> showEpisodes = new ArrayList<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
        name = "media_genres",
        joinColumns = @JoinColumn(name = "media_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id"),
        uniqueConstraints = @UniqueConstraint(name = "uq_media_genres_media_genre", columnNames = {"media_id", "genre_id"})
    )
    private List<Genre> genres = new ArrayList<>();

    @OneToMany(mappedBy = "media")
    private List<Review> reviews;
}




