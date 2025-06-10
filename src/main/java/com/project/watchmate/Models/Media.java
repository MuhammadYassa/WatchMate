package com.project.watchmate.Models;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
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

    private Long tmdbId;

    private String title;

    private String overview;

    private String posterPath;

    private Date releaseDate;

    private MediaType type;

    @ManyToMany
    @JoinTable(
        name = "media_genres",
        joinColumns = @JoinColumn(name = "media_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private List<Genre> genres; 

    @OneToMany(mappedBy = "media")
    private List<Review> reviews;
}
