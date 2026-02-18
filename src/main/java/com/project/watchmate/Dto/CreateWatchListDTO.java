package com.project.watchmate.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateWatchListDTO {
    
    @NotBlank
    @Size(max = 50)
    private String name;
    
}
