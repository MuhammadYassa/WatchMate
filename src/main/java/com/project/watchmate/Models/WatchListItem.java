package com.project.watchmate.Models;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Builder
@Entity
@Table(
    name = "watchlist_items",
    uniqueConstraints = @UniqueConstraint(name = "uq_watchlist_items_watchlist_media", columnNames = {"watchlist_id", "media_id"})
)
public class WatchListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watchlist_id", nullable = false)
    private WatchList watchList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    private LocalDateTime addedAt;

    private Integer position;

}
