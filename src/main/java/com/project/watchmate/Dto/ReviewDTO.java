package com.project.watchmate.Dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {

    private String username;

    private String comment;

    private int starRating;

    private LocalDateTime postedAt;

}
