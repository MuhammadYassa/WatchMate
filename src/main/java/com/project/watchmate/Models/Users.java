package com.project.watchmate.Models;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
@Builder
public class Users {
    @Id
    @GeneratedValue
    private Integer id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;
    
    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private boolean emailVerified;
    
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WatchList> watchLists = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserMediaStatus> mediaStatuses = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "user_favorites")
    private List<Media> favorites;
    
    @ManyToMany
    @JoinTable(
        name = "user_following",
        joinColumns = @JoinColumn(name = "follower_id"),
        inverseJoinColumns = @JoinColumn(name = "following_id")
    )
    private List<Users> following;

    @ManyToMany(mappedBy = "following")
    private List<Users> followers;
}
