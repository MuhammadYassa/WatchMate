package com.project.watchmate.watchlist.domain;

import java.util.ArrayList;
import java.util.List;

import com.project.watchmate.user.domain.Users;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "watch_list",
    uniqueConstraints = @UniqueConstraint(name = "uk_watchlist_user_name", columnNames = {"user_id", "normalized_name"})
)
@Builder
public class WatchList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(
        name = "normalized_name",
        insertable = false,
        updatable = false,
        columnDefinition = "varchar(255) generated always as (lower(name)) stored"
    )
    private String normalizedName;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Builder.Default
    @OneToMany(mappedBy = "watchList", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WatchListItem> items = new ArrayList<>();

}


