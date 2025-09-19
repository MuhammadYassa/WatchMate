package com.project.watchmate.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT u FROM Users u LEFT JOIN FETCH u.following WHERE u.id = :userId")
    Optional<Users> findByIdWithFollowing(@Param("userId") Long userId);

    @Query("SELECT u FROM Users u LEFT JOIN FETCH u.followers WHERE u.id = :userId")
    Optional<Users> findByIdWithFollowers(@Param("userId") Long userId);

    @Query("SELECT u FROM Users u LEFT JOIN FETCH u.following LEFT JOIN FETCH u.followers WHERE u.id = :userId")
    Optional<Users> findByIdWithFollowRelations(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) > 0 FROM Users u JOIN u.following f WHERE u.id = :followerId AND f.id = :targetId")
    boolean isFollowing(@Param("followerId") Long followerId, @Param("targetId") Long targetId);
    
    @Query("SELECT COUNT(b) > 0 FROM Users u JOIN u.blockedUsers b WHERE u.id = :userId AND b.id = :blockedUserId")
    boolean isBlockedByUser(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);
    
    @Query("SELECT COUNT(b) > 0 FROM Users u JOIN u.blockedUsers b WHERE u.id = :blockerId AND b.id = :targetId")
    boolean isBlockingUser(@Param("blockerId") Long blockerId, @Param("targetId") Long targetId);
}
