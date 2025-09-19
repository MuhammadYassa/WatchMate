package com.project.watchmate.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.watchmate.Models.FollowRequest;
import com.project.watchmate.Models.FollowRequestStatuses;
import com.project.watchmate.Models.Users;

public interface FollowRequestRepository extends JpaRepository<FollowRequest, Long> {
    List<FollowRequest> findByTargetUserAndStatus(Users targetUser, FollowRequestStatuses status);
    Optional<FollowRequest> findByRequestUserAndTargetUser(Users requestUser, Users targetUser);
    boolean existsByRequestUserAndTargetUserAndStatus(Users requestUser, Users targetUser, FollowRequestStatuses status);
}
