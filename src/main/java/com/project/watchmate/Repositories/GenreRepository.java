package com.project.watchmate.Repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.watchmate.Models.Genre;
import com.project.watchmate.Models.MediaType;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    List<Genre> findByMediaTypeOrderByNameAsc(MediaType mediaType);

    @Query("""
        select genre
        from Genre genre
        where genre.mediaType = :mediaType
            and genre.syncedAt = (
                select max(currentGenre.syncedAt)
                from Genre currentGenre
                where currentGenre.mediaType = :mediaType
            )
        order by genre.name asc
        """)
    List<Genre> findCurrentByMediaTypeOrderByNameAsc(@Param("mediaType") MediaType mediaType);

    @Query("""
        select genre
        from Genre genre
        where lower(genre.name) = lower(:name)
            and genre.mediaType = :mediaType
            and genre.syncedAt = (
                select max(currentGenre.syncedAt)
                from Genre currentGenre
                where currentGenre.mediaType = :mediaType
            )
        """)
    Optional<Genre> findCurrentByNameIgnoreCaseAndMediaType(@Param("name") String name, @Param("mediaType") MediaType mediaType);

    List<Genre> findByTmdbGenreIdInAndMediaType(Collection<Long> tmdbGenreIds, MediaType mediaType);
}
