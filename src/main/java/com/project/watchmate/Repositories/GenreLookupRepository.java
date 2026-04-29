package com.project.watchmate.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.watchmate.Models.GenreLookup;
import com.project.watchmate.Models.MediaType;

public interface GenreLookupRepository extends JpaRepository<GenreLookup, Long> {

    Optional<GenreLookup> findByNameIgnoreCaseAndMediaType(String name, MediaType mediaType);

    @Query("select distinct genreLookup.name from GenreLookup genreLookup order by genreLookup.name asc")
    List<String> findDistinctNamesOrderByNameAsc();

    @Modifying
    @Query("delete from GenreLookup genreLookup where genreLookup.mediaType = :mediaType")
    void deleteByMediaType(@Param("mediaType") MediaType mediaType);
}
