package com.project.watchmate.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.watchmate.Models.EmailVerificationToken;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    EmailVerificationToken getByToken(String token);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
