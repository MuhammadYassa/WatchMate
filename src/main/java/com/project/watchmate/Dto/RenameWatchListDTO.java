package com.project.watchmate.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "RenameWatchListRequest", description = "Payload used to rename a watchlist.")
public class RenameWatchListDTO {

    @NotBlank
    @Size(max = 50)
    @Schema(description = "New display name for the watchlist.", example = "Award Winners", maxLength = 50)
    private String newName;
    
}
