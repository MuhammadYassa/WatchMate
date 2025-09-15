package com.project.watchmate.Dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowListDTO {

    private List<FollowListUserDetailsDTO> followList;
}
