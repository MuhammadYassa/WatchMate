package com.project.watchmate.Services;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.Dto.FollowListUserDetailsDTO;
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
import com.project.watchmate.Mappers.WatchMateMapper;
import com.project.watchmate.Models.FollowRequest;
import com.project.watchmate.Models.FollowRequestStatuses;
import com.project.watchmate.Models.FollowStatuses;
import com.project.watchmate.Models.PrivacyStatuses;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Repositories.FollowRequestRepository;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocialService {

    private final UsersRepository usersRepository;

    private final FollowRequestRepository followRequestRepository;

    private final WatchMateMapper watchMateMapper;

    private final WatchListService watchListService;

    private final UserMediaStatusRepository userMediaStatusRepository;

    private final MediaService mediaService;

    private final ReviewService reviewService;

    private Users findAndValidateTargetUser(Long userId){
        return usersRepository.findById(Objects.requireNonNull(userId, "userId"))
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
        followRequestRepository.save(Objects.requireNonNull(FollowRequest.builder()
        .targetUser(targetUser)
        .requestUser(user)
        .requestedAt(LocalDateTime.now())
        .status(FollowRequestStatuses.PENDING)
        .build()));
        return FollowStatusDTO.builder()
        .followStatus(FollowStatuses.NOT_FOLLOWING)
        .build();
    }

    @Transactional
    public FollowRequestResponseDTO respondToFollowRequest(Long requestId, Users user, FollowRequestStatuses response) {
        FollowRequest request = followRequestRepository.findById(Objects.requireNonNull(requestId, "requestId"))
            .orElseThrow(() -> new FollowRequestNotFoundException("Request not found"));
        
        if (response == FollowRequestStatuses.CANCELED) {
            if (!request.getRequestUser().equals(user)) {
                throw new UnauthorizedFollowRequestAccessException("You can only cancel your own requests");
            }
            followRequestRepository.delete(request);
            return FollowRequestResponseDTO.builder()
            .newStatus(FollowRequestStatuses.CANCELED)
            .requestId(Objects.requireNonNull(requestId, "requestId"))
            .build();           
        } else {
            if (!request.getTargetUser().equals(user)) {
                throw new UnauthorizedFollowRequestAccessException("Not your request");
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

    @Transactional(readOnly = true)
    public Page<FollowRequestDTO> getReceivedRequests(Users user, int pageNumber, int size) {
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by("requestedAt").descending().and(Sort.by("id").descending()));
        Page <FollowRequest> page = followRequestRepository.findByTargetUserAndStatus(user, FollowRequestStatuses.PENDING, pageable);

        return page.map(followRequest -> watchMateMapper.mapToFollowRequestDTO(followRequest));
    }

    @Transactional
    public FollowStatusDTO getFollowStatus(Long userId, Users user) {
        if (user.getId() == userId){
            return FollowStatusDTO.builder()
           .followStatus(FollowStatuses.NOT_FOLLOWING)
           .build();
        }
        Users targetUser = findAndValidateTargetUser(userId);
        if (usersRepository.isBlockedByUser(user.getId(), targetUser.getId()) || usersRepository.isBlockingUser(targetUser.getId(), user.getId())){
            return FollowStatusDTO.builder()
            .followStatus(FollowStatuses.BLOCKED)
            .build();
        }
        if (usersRepository.isFollowing(user.getId(), targetUser.getId())){
            return FollowStatusDTO.builder()
            .followStatus(FollowStatuses.FOLLOWING)
            .build();
        }
        return FollowStatusDTO.builder()
        .followStatus(FollowStatuses.NOT_FOLLOWING)
        .build();
    }

    @Transactional
    public FollowStatusDTO blockUser(Long userId, Users user) {
        Users targetUser = findAndValidateTargetUser(userId);
        if (user.getId() == userId){
            throw new SelfFollowException("Cannot block yourself!");
        }
        if (usersRepository.isBlockingUser(user.getId(), targetUser.getId())){
            user.getBlockedUsers().remove(targetUser);
            usersRepository.save(user);
            return FollowStatusDTO.builder()
            .followStatus(FollowStatuses.NOT_FOLLOWING)
            .build();
        } else {
            user.getBlockedUsers().add(targetUser);
            usersRepository.deleteFollowRelation(user.getId(), targetUser.getId());
            usersRepository.deleteFollowRelation(targetUser.getId(), user.getId());
            followRequestRepository.deleteByRequestUserAndTargetUser(user, targetUser);
            followRequestRepository.deleteByRequestUserAndTargetUser(targetUser, user);
            return FollowStatusDTO.builder()
            .followStatus(FollowStatuses.BLOCKED)
            .build();
        }
    }

    @Transactional(readOnly = true)
    public Page<FollowListUserDetailsDTO> getFollowersList(Users user, int pageNumber, int size) {
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by("username").ascending().and(Sort.by("id").descending()));
        Page<Users> page = usersRepository.findFollowersByUser(user, pageable);
        return page.map(u -> new FollowListUserDetailsDTO(u.getUsername()));
    }

    @Transactional(readOnly = true)
    public Page<FollowListUserDetailsDTO> getFollowingList(Users user, int pageNumber, int size) {
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by("username").ascending().and(Sort.by("id").descending()));
        Page<Users> page = usersRepository.findFollowingByUser(user, pageable);
        return page.map(u -> new FollowListUserDetailsDTO(u.getUsername()));
    }


    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(Long userId, Users user) {
        Users targetUser = findAndValidateTargetUser(userId);
        if(user.getId() == targetUser.getId()){
            return retrieveSelfUserProfile(targetUser);
        }
        if(usersRepository.isBlockedByUser(targetUser.getId(), user.getId()) || usersRepository.isBlockingUser(user.getId(), targetUser.getId())){
            return UserProfileDTO.builder()
            .username(targetUser.getUsername())
            .followersCount(0L)
            .followingCount(0L)
            .followStatus(FollowStatuses.BLOCKED)
            .privacyStatus(targetUser.getPrivacyStatus())
            .build();
        }
        return retrieveTargetUserProfile(user, targetUser);
    }
    
    private UserProfileDTO retrieveSelfUserProfile(Users user){
        return UserProfileDTO.builder()
            .username(user.getUsername())
            .privacyStatus(user.getPrivacyStatus())
            .followStatus(FollowStatuses.NOT_FOLLOWING)
            .followersCount(usersRepository.countFollowersByUserId(user.getId()))
            .followingCount(usersRepository.countFollowingByUserId(user.getId()))
            .watchlists((watchListService.getWatchListPage(user)).map(watchList -> watchListService.mapToWatchListDTO(watchList)))
            .reviews(reviewService.getReviewPage(user).map(review -> watchMateMapper.mapToReviewDTO(review)))
            .moviesWatchedCount(userMediaStatusRepository.countWatchedMoviesByUser(user))
            .showsWatchedCount(userMediaStatusRepository.countWatchedShowsByUser(user))
            .moviesWatched(mediaService.getMoviesWatchedPage(user).map(m -> watchMateMapper.mapToSearchItemDTO(m)))
            .showsWatched(mediaService.getShowsWatchedPage(user).map(m -> watchMateMapper.mapToSearchItemDTO(m)))
            .build();
    }

    private UserProfileDTO retrieveTargetUserProfile(Users user, Users targetUser){
        if (usersRepository.isFollowing(user.getId(), targetUser.getId()) || targetUser.getPrivacyStatus() == PrivacyStatuses.PUBLIC){
            return UserProfileDTO.builder()
                .username(targetUser.getUsername())
                .privacyStatus(targetUser.getPrivacyStatus())
                .followStatus(FollowStatuses.NOT_FOLLOWING)
                .followersCount(usersRepository.countFollowersByUserId(targetUser.getId()))
                .followingCount(usersRepository.countFollowingByUserId(targetUser.getId()))
                .watchlists((watchListService.getWatchListPage(targetUser)).map(watchList -> watchListService.mapToWatchListDTO(watchList)))
                .moviesWatchedCount(userMediaStatusRepository.countWatchedMoviesByUser(targetUser))
                .showsWatchedCount(userMediaStatusRepository.countWatchedShowsByUser(targetUser))
                .build();
        } else {
            return UserProfileDTO.builder()
                .username(targetUser.getUsername())
                .privacyStatus(targetUser.getPrivacyStatus())
                .followStatus(followRequestRepository.existsByRequestUserAndTargetUserAndStatus(user, targetUser, FollowRequestStatuses.PENDING)? FollowStatuses.REQUESTED : FollowStatuses.NOT_FOLLOWING)
                .followersCount(usersRepository.countFollowersByUserId(targetUser.getId()))
                .followingCount(usersRepository.countFollowingByUserId(targetUser.getId()))
                .build();
        }
    }
}
