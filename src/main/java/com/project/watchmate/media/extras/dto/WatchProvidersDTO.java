package com.project.watchmate.media.extras.dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "WatchProviders", description = "Watch-provider availability for the configured region.")
public class WatchProvidersDTO {

    @Schema(description = "Configured provider region.", example = "US")
    private String region;

    @Schema(description = "TMDB/JustWatch link for the configured region. Null when unavailable.")
    private String link;

    @Builder.Default
    @Schema(description = "Subscription streaming providers. Empty when unavailable.")
    private List<WatchProviderEntryDTO> flatrate = new ArrayList<>();

    @Builder.Default
    @Schema(description = "Digital rental providers. Empty when unavailable.")
    private List<WatchProviderEntryDTO> rent = new ArrayList<>();

    @Builder.Default
    @Schema(description = "Digital purchase providers. Empty when unavailable.")
    private List<WatchProviderEntryDTO> buy = new ArrayList<>();

    @Builder.Default
    @Schema(description = "Free-with-ads providers. Empty when unavailable.")
    private List<WatchProviderEntryDTO> ads = new ArrayList<>();

    @Builder.Default
    @Schema(description = "Free providers. Empty when unavailable.")
    private List<WatchProviderEntryDTO> free = new ArrayList<>();
}
