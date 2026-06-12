package com.project.watchmate.user.persistence;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.watchmate.user.domain.Users;
import com.project.watchmate.social.dto.SearchListUserDetailsDTO;

public interface UsersRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByUsername(String username);

    Optional<Users> findByUsernameIgnoreCase(String username);

    Optional<Users> findByUsernameIgnoreCaseAndEmailVerifiedTrue(String username);

    Optional<Users> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("select count(f) from Users u join u.followers f where u.id = :userId")
    long countFollowersByUserId(@Param("userId") Long userId);

    @Query("select count(f) from Users u join u.following f where u.id = :userId")
    long countFollowingByUserId(@Param("userId") Long userId);

    @Query("select f from Users u join u.followers f where u = :user order by f.username asc, f.id desc")
    Page<Users> findFollowersByUser(@Param("user") Users user, Pageable pageable);

    @Query("select f from Users u join u.following f where u = :user order by f.username asc, f.id desc")
    Page<Users> findFollowingByUser(@Param("user") Users user, Pageable pageable);

    @Query("SELECT DISTINCT u FROM Users u LEFT JOIN FETCH u.favorites WHERE u.id = :userId")
    Optional<Users> findByIdWithFavorites(@Param("userId") Long userId);

    @Query("select favorite.id from Users u join u.favorites favorite where u.id = :userId and favorite.id in :mediaIds")
    List<Long> findFavoriteMediaIds(@Param("userId") Long userId, @Param("mediaIds") Collection<Long> mediaIds);

    @Query("SELECT COUNT(f) > 0 FROM Users u JOIN u.following f WHERE u.id = :followerId AND f.id = :targetId")
    boolean isFollowing(@Param("followerId") Long followerId, @Param("targetId") Long targetId);
    
    @Query("SELECT COUNT(b) > 0 FROM Users u JOIN u.blockedUsers b WHERE u.id = :userId AND b.id = :blockedUserId")
    boolean isBlockedByUser(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);
    
    @Query("SELECT COUNT(b) > 0 FROM Users u JOIN u.blockedUsers b WHERE u.id = :blockerId AND b.id = :targetId")
    boolean isBlockingUser(@Param("blockerId") Long blockerId, @Param("targetId") Long targetId);

    @Query("""
        SELECT new com.project.watchmate.social.dto.SearchListUserDetailsDTO(
            u.username,
            CASE WHEN follower.id IS NOT NULL THEN true ELSE false END,
            CASE WHEN u.id = :viewerId THEN true ELSE false END,
            u.privacyStatus
        )
        FROM Users u
        LEFT JOIN u.followers follower ON follower.id = :viewerId
        WHERE u.emailVerified = true
          AND LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
          AND NOT EXISTS (
              SELECT 1
              FROM Users viewer
              JOIN viewer.blockedUsers blocked
              WHERE viewer.id = :viewerId AND blocked.id = u.id
          )
          AND NOT EXISTS (
              SELECT 1
              FROM Users candidate
              JOIN candidate.blockedUsers blockedViewer
              WHERE candidate.id = u.id AND blockedViewer.id = :viewerId
          )
        ORDER BY
          CASE
              WHEN LOWER(u.username) = LOWER(:query) THEN 0
              WHEN follower.id IS NOT NULL AND LOWER(u.username) LIKE LOWER(CONCAT(:query, '%')) THEN 1
              WHEN LOWER(u.username) LIKE LOWER(CONCAT(:query, '%')) THEN 2
              WHEN follower.id IS NOT NULL THEN 3
              ELSE 4
          END,
          LOWER(u.username) ASC,
          u.id ASC
        """)
    List<SearchListUserDetailsDTO> searchByUsername(@Param("query") String query, @Param("viewerId") Long viewerId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "delete from user_following where follower_id = :followerId and following_id = :followingId", nativeQuery = true)
    int deleteFollowRelation(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    @Modifying
    @Query(value = "insert into user_following (follower_id, following_id) values (:followerId, :followingId)", nativeQuery = true)
    void insertFollowRelation(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    @Modifying
    @Query(value = "insert into blocked_users (blocker_id, blocked_id) values (:userId, :blockedUserId)", nativeQuery = true)
    void insertBlockRelation(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);

    @Modifying
    @Query(value = "delete from blocked_users where blocker_id = :userId and blocked_id = :blockedUserId", nativeQuery = true)
    void deleteBlockRelation(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);

}



