package com.project.watchmate.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.watchmate.Models.Users;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {
    Users findByUsername(String username);

    Users findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByFollowerAndFollowing(Users follower, Users following);

    Long countFollowers(Users user);

    Long countFollowing(Users user);

    List<Users> findFollowersByUser(Users user);

    List<Users> findFollowingByUser(Users user);
}
