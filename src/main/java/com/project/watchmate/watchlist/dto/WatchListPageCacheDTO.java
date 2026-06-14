package com.project.watchmate.watchlist.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchListPageCacheDTO {

    private List<WatchListDTO> content;

    private int page;

    private int size;

    private long totalElements;
}
