package com.project.watchmate.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "CreateWatchListRequest", description = "Payload used to create a watchlist.")
public class CreateWatchListDTO {
    
    @NotBlank
    @Size(max = 50)
    @Schema(description = "Display name of the watchlist.", example = "Weekend Movies", maxLength = 50)
    private String name;
    
}
