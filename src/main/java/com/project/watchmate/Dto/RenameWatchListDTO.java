package com.project.watchmate.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RenameWatchListDTO {

    @NotBlank
    @Size(max = 50)
    private String newName;
    
}
