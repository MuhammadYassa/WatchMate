package com.project.watchmate.Models;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Genre {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "tmdb_id", unique = true)
    private Long tmdbId;

    private String genre;

    @ManyToMany(mappedBy = "genre")
    private List<Media> media;
}
