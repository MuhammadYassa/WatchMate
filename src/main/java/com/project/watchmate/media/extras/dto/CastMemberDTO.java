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
@Schema(name = "CastMember", description = "Public cast member information from TMDB.")
public class CastMemberDTO {

    @Schema(description = "TMDB person identifier.", example = "287")
    private Long tmdbPersonId;

    @Schema(description = "Cast member display name.", example = "Brad Pitt")
    private String name;

    @Schema(description = "Character or role name.", example = "Tyler Durden")
    private String character;

    @Schema(description = "Relative TMDB profile image path. Null when unavailable.", example = "/kU3B75TyRiCgE270EyZnHjfivoq.jpg")
    private String profilePath;

    @Schema(description = "TMDB billing order. Lower values are more prominent.", example = "0")
    private Integer order;

    @Schema(description = "TMDB known-for department.", example = "Acting")
    private String knownForDepartment;
}
