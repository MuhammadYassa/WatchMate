package com.project.watchmate.Models;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@Table
@NoArgsConstructor
@AllArgsConstructor
public class Genre {

    @Id
    private Long id;

    private String name;

    @ManyToMany(mappedBy = "genres")
    private List<Media> media;
    
}
