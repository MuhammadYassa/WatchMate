package com.project.watchmate.social.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.project.watchmate.social.domain.FollowRequest;
import com.project.watchmate.social.domain.FollowRequestStatuses;
import com.project.watchmate.user.domain.Users;

public interface FollowRequestRepository extends JpaRepository<FollowRequest, Long> {

    Page<FollowRequest> findByTargetUserAndStatus(Users targetUser, FollowRequestStatuses status, Pageable pageable);

    Optional<FollowRequest> findByRequestUserAndTargetUser(Users requestUser, Users targetUser);

    boolean existsByRequestUserAndTargetUserAndStatus(Users requestUser, Users targetUser, FollowRequestStatuses status);

    void deleteByRequestUserAndTargetUser(Users requestUser, Users targetUser);
    
}



