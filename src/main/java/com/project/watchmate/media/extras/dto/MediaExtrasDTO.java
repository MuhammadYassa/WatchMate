package com.project.watchmate.media.extras.dto;

import java.util.List;

public record MediaExtrasDTO(
    List<CastMemberDTO> cast,
    TrailerDTO bestTrailer,
    WatchProvidersDTO watchProviders
) {
}
