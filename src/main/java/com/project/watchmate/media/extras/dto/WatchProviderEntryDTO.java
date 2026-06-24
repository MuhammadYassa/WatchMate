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
@Schema(name = "WatchProviderEntry", description = "One streaming, rental, purchase, ad-supported, or free provider.")
public class WatchProviderEntryDTO {

    @Schema(description = "TMDB provider identifier.", example = "337")
    private Integer providerId;

    @Schema(description = "Provider display name.", example = "Disney Plus")
    private String providerName;

    @Schema(description = "Relative TMDB provider logo path.", example = "/7rwgEs15tFwyR9NPQ5vpzxTj19d.jpg")
    private String logoPath;

    @Schema(description = "TMDB display priority. Lower values are more prominent.", example = "0")
    private Integer displayPriority;
}
