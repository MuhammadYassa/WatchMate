package com.project.watchmate.Services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.project.watchmate.Dto.FollowListDTO;
import com.project.watchmate.Dto.FollowRequestDTO;
import com.project.watchmate.Dto.FollowRequestResponseDTO;
import com.project.watchmate.Dto.FollowStatusDTO;
import com.project.watchmate.Dto.UserProfileDTO;
import com.project.watchmate.Exception.AlreadyFollowingException;
import com.project.watchmate.Exception.BlockedUserException;
import com.project.watchmate.Exception.FollowRequestNotFoundException;
import com.project.watchmate.Exception.NotFollowingException;
import com.project.watchmate.Exception.SelfFollowException;
import com.project.watchmate.Exception.UnauthorizedFollowRequestAccessException;
import com.project.watchmate.Exception.UserNotFoundException;
import com.project.watchmate.Models.FollowRequest;
import com.project.watchmate.Models.FollowRequestStatuses;
import com.project.watchmate.Models.FollowStatuses;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Repositories.FollowRequestRepository;
import com.project.watchmate.Repositories.UsersRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocialService {

    private final UsersRepository usersRepository;

    private final FollowRequestRepository followRequestRepository;

    private Users findAndValidateTargetUser(Long userId){
        return usersRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private void validateFollowEligibility(Users user, Users targetUser){
        if (user.getId().equals(targetUser.getId())){
            throw new SelfFollowException("You cannot follow yourself!");
        }

        if (usersRepository.isFollowing(user.getId(), targetUser.getId())){
            throw new AlreadyFollowingException("User already following!");
        }

        if (usersRepository.isBlockedByUser(targetUser.getId(), user.getId())){
            throw new BlockedUserException("User is blocked by Target User!");
        }

        if (usersRepository.isBlockingUser(user.getId(), targetUser.getId())){
            throw new BlockedUserException("Target user is blocked by User!");
        }
    }

    private void validateUnfollowEligibility(Users user, Users targetUser){
        if (user.getId().equals(targetUser.getId())){
            throw new SelfFollowException("You cannot unfollow yourself!");
        }

        if (!usersRepository.isFollowing(user.getId(), targetUser.getId())){
            throw new NotFollowingException("Not following target user!");
        }

        if (usersRepository.isBlockedByUser(targetUser.getId(), user.getId())){
            throw new BlockedUserException("User is blocked by Target User!");
        }

        if (usersRepository.isBlockingUser(user.getId(), targetUser.getId())){
            throw new BlockedUserException("Target user is blocked by User!");
        }
    }

    private FollowStatusDTO handleFollowBasedOnPrivacy(Users user, Users targetUser){
        return switch (targetUser.getPrivacyStatus()) {
            case PUBLIC -> performDirectFollow(user, targetUser);
            case PRIVATE -> createFollowRequest(user, targetUser);
        };
    }

    @Transactional
    private FollowStatusDTO handleUnfollow(Users user, Users targetUser){
        user.getFollowing().remove(targetUser);
        targetUser.getFollowers().remove(user);

        usersRepository.save(user);
        usersRepository.save(targetUser);

        return FollowStatusDTO.builder()
        .followStatus(FollowStatuses.NOT_FOLLOWING)
        .build();
    }

    @Transactional
    private FollowStatusDTO performDirectFollow(Users user, Users targetUser){
        targetUser.getFollowers().add(user);
        user.getFollowing().add(targetUser);

        usersRepository.save(user);
        usersRepository.save(targetUser);

        return FollowStatusDTO.builder()
        .followStatus(FollowStatuses.FOLLOWING)
        .build();
    }

    @Transactional
    private FollowStatusDTO createFollowRequest(Users user, Users targetUser){
        if (followRequestRepository.existsByRequestUserAndTargetUserAndStatus(user, targetUser, FollowRequestStatuses.PENDING)) {
            throw new AlreadyFollowingException("Follow request already pending");
        }
        followRequestRepository.save(FollowRequest.builder()
        .targetUser(targetUser)
        .requestUser(user)
        .requestedAt(LocalDateTime.now())
        .status(FollowRequestStatuses.PENDING)
        .build());
        return FollowStatusDTO.builder()
        .followStatus(FollowStatuses.NOT_FOLLOWING)
        .build();
    }

    @Transactional
    public FollowRequestResponseDTO respondToFollowRequest(Long requestId, Users user, FollowRequestStatuses response) {
        FollowRequest request = followRequestRepository.findById(requestId)
            .orElseThrow(() -> new FollowRequestNotFoundException("Request not found"));
        
        if (!request.getTargetUser().equals(user)) {
            throw new UnauthorizedFollowRequestAccessException("Not your request");
        }

        if (response == FollowRequestStatuses.CANCELED) {
            if (!request.getRequestUser().equals(user)) {
                throw new UnauthorizedFollowRequestAccessException("You can only cancel your own requests");
            }
            followRequestRepository.delete(request);
            return FollowRequestResponseDTO.builder()
            .newStatus(FollowRequestStatuses.CANCELED)
            .requestId(requestId)
            .build();           
        }
        
        request.setStatus(response);
        request.setRespondedAt(LocalDateTime.now());
        
        if (response == FollowRequestStatuses.ACCEPTED) {
            performDirectFollow(request.getRequestUser(), request.getTargetUser());
        }
        
        followRequestRepository.save(request);
        return FollowRequestResponseDTO.builder()
            .newStatus(response)
            .build();
    }
    
    @Transactional
    public FollowStatusDTO followUser(Long userId, Users user) {
        Users targetUser = findAndValidateTargetUser(userId);
        validateFollowEligibility(user, targetUser);
        
        return handleFollowBasedOnPrivacy(user, targetUser);
    }

    @Transactional
    public FollowStatusDTO unfollowUser(Long userId, Users user) {
        Users targetUser = findAndValidateTargetUser(userId);
        validateUnfollowEligibility(user, targetUser);

        return handleUnfollow(user, targetUser);
    }

    public List<FollowRequestDTO> getReceivedRequests(Users user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'allRequests'");
    }

    public FollowStatusDTO getFollowStatus(Long userId, Users user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFollowStatus'");
    }

    public FollowStatusDTO blockUser(Long userId, Users user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'blockUser'");
    }

    public FollowListDTO getFollowersList(Users user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFollowersList'");
    }

    public UserProfileDTO getUserProfile(Long userId, Users user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserProfile'");
    }

    public FollowListDTO getFollowingList(Users user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFollowingList'");
    }

}
