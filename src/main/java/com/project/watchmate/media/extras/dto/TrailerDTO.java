package com.project.watchmate.media.extras.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "Trailer", description = "Best available YouTube trailer or teaser.")
public class TrailerDTO {

    @Schema(description = "YouTube video key.", example = "W-ZGXoZVQpI")
    private String key;

    @Schema(description = "Video title from TMDB.", example = "Fight Club | Official Trailer")
    private String name;

    @Schema(description = "Video hosting site.", example = "YouTube")
    private String site;

    @Schema(description = "TMDB video type.", example = "Trailer")
    private String type;

    @Schema(description = "Whether TMDB marks this video official.", example = "true")
    private Boolean official;

    @Schema(description = "TMDB published timestamp as an ISO-8601 string.", example = "2022-09-28T17:44:29.000Z")
    private String publishedAt;

    @Schema(description = "Direct YouTube watch URL.", example = "https://www.youtube.com/watch?v=W-ZGXoZVQpI")
    private String youtubeUrl;

    @Schema(description = "YouTube thumbnail URL.", example = "https://img.youtube.com/vi/W-ZGXoZVQpI/hqdefault.jpg")
    private String thumbnailUrl;
}
