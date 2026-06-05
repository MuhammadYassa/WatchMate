package com.project.watchmate.media.tmdb.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TmdbGenreResponseDTO {
    
    private List<TmdbGenreDTO> genres;
    
}


