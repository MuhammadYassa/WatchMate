package com.project.watchmate.Models;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uq_users_email", columnNames = "email"),
    @UniqueConstraint(name = "uq_users_username", columnNames = "username")
})
@Builder
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private boolean emailVerified;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private PrivacyStatuses privacyStatus = PrivacyStatuses.PUBLIC;
    
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
    @JoinTable(
        name = "user_favorites",
        joinColumns = @JoinColumn(name = "users_id"),
        inverseJoinColumns = @JoinColumn(name = "favorites_id"),
        uniqueConstraints = @UniqueConstraint(name = "uq_user_favorites_user_media", columnNames = {"users_id", "favorites_id"})
    )
    private List<Media> favorites;
    
    @Builder.Default
    @ManyToMany
    @JoinTable(
        name = "user_following",
        joinColumns = @JoinColumn(name = "follower_id"),
        inverseJoinColumns = @JoinColumn(name = "following_id"),
        uniqueConstraints = @UniqueConstraint(name = "uq_user_following_follower_following", columnNames = {"follower_id", "following_id"})
    )
    private List<Users> following = new ArrayList<>();

    @Builder.Default
    @ManyToMany(mappedBy = "following")
    private List<Users> followers = new ArrayList<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
        name = "blocked_users",
        joinColumns = @JoinColumn(name = "blocker_id"),
        inverseJoinColumns = @JoinColumn(name = "blocked_id"),
        uniqueConstraints = @UniqueConstraint(name = "uq_blocked_users_blocker_blocked", columnNames = {"blocker_id", "blocked_id"})
    )
    private List<Users> blockedUsers = new ArrayList<>();
    
}
