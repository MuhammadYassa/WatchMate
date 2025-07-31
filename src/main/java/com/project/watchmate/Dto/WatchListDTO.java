package com.project.watchmate.Dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WatchListDTO {

    private Long id;

    private String name;

    private List<MediaDetailsDTO> media;
    
}
