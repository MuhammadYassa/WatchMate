package com.project.watchmate.social.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.social.dto.FollowListUserDetailsDTO;
import com.project.watchmate.social.dto.FollowRequestDTO;
import com.project.watchmate.social.dto.FollowRequestResponseDTO;
import com.project.watchmate.social.dto.FollowStatusDTO;
import com.project.watchmate.social.dto.SearchListUserDetailsDTO;
import com.project.watchmate.social.dto.UpdateProfileRequestDTO;
import com.project.watchmate.social.dto.UpdateProfileResponseDTO;
import com.project.watchmate.social.dto.UserProfileDTO;
import com.project.watchmate.common.error.AlreadyFollowingException;
import com.project.watchmate.common.error.BlockedUserException;
import com.project.watchmate.common.error.FollowRequestNotFoundException;
import com.project.watchmate.common.error.PrivateProfileException;
import com.project.watchmate.review.dto.ReviewResponseDTO;
import com.project.watchmate.common.error.FollowRequestStateConflictException;
import com.project.watchmate.common.error.NotFollowingException;
import com.project.watchmate.common.error.SelfFollowException;
import com.project.watchmate.common.error.UnauthorizedFollowRequestAccessException;
import com.project.watchmate.common.error.UserNotFoundException;
import com.project.watchmate.common.mapper.WatchMateMapper;
import com.project.watchmate.movie.application.MediaService;
import com.project.watchmate.review.application.ReviewService;
import com.project.watchmate.social.domain.FollowRequest;
import com.project.watchmate.social.domain.FollowRequestStatuses;
import com.project.watchmate.social.domain.FollowStatuses;
import com.project.watchmate.user.domain.PrivacyStatuses;
import com.project.watchmate.user.domain.Role;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.social.persistence.FollowRequestRepository;
import com.project.watchmate.user.persistence.UsersRepository;
import com.project.watchmate.watchlist.application.WatchListService;
import com.project.watchmate.watchlist.domain.WatchList;
import com.project.watchmate.watchlist.dto.WatchListDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SocialService {

    private final UsersRepository usersRepository;

    private final FollowRequestRepository followRequestRepository;

    private final WatchMateMapper watchMateMapper;

    private final WatchListService watchListService;

    private final MediaService mediaService;

    private final ReviewService reviewService;

    private Users findAndValidateTargetUser(Long userId){
        return usersRepository.findByIdAndEmailVerifiedTrue(Objects.requireNonNull(userId, "userId"))
        .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private Users findAndValidateTargetUser(String username) {
        return usersRepository.findByUsernameIgnoreCaseAndEmailVerifiedTrue(Objects.requireNonNull(username, "username"))
            .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private void validateFollowEligibility(Users user, Users targetUser){
        if (user.getId().equals(targetUser.getId())){
            throw new SelfFollowException("You cannot follow yourself!");
        }

        if (usersRepository.isFollowing(user.getId(), targetUser.getId())){
            throw new AlreadyFollowingException("User already following!");
        }

        if (usersRepository.isBlockingUser(targetUser.getId(), user.getId())){
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

        if (usersRepository.isBlockingUser(targetUser.getId(), user.getId())){
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

    private FollowStatusDTO handleUnfollow(Users user, Users targetUser){
        usersRepository.deleteFollowRelation(user.getId(), targetUser.getId());

        log.info("User unfollowed target username={} targetUsername={}", user.getUsername(), targetUser.getUsername());

        return FollowStatusDTO.builder()
        .followStatus(FollowStatuses.NOT_FOLLOWING)
        .build();
    }

    private void insertFollowRelation(Long followerId, Long followingId, RuntimeException conflictException) {
        try {
            usersRepository.insertFollowRelation(followerId, followingId);
        } catch (DataIntegrityViolationException ex) {
            throw conflictException;
        }
    }

    private FollowStatusDTO performDirectFollow(Users user, Users targetUser){
        insertFollowRelation(user.getId(), targetUser.getId(),
            new AlreadyFollowingException("User already following!"));

        log.info("User followed target username={} targetUsername={}", user.getUsername(), targetUser.getUsername());

        return FollowStatusDTO.builder()
        .followStatus(FollowStatuses.FOLLOWING)
        .build();
    }

    private boolean hasPendingFollowRequest(Users user, Users targetUser) {
        return followRequestRepository.existsByRequestUserAndTargetUserAndStatus(user, targetUser, FollowRequestStatuses.PENDING);
    }

    private FollowStatusDTO createFollowRequest(Users user, Users targetUser){
        if (hasPendingFollowRequest(user, targetUser)) {
            throw new AlreadyFollowingException("Follow request already pending");
        }
        followRequestRepository.save(Objects.requireNonNull(FollowRequest.builder()
        .targetUser(targetUser)
        .requestUser(user)
        .requestedAt(LocalDateTime.now())
        .status(FollowRequestStatuses.PENDING)
        .build()));
        log.info("Follow request created username={} targetUsername={}", user.getUsername(), targetUser.getUsername());
        return FollowStatusDTO.builder()
        .followStatus(FollowStatuses.REQUESTED)
        .build();
    }

    private void validatePendingFollowRequestState(FollowRequest request) {
        if (request.getStatus() != FollowRequestStatuses.PENDING) {
            throw new FollowRequestStateConflictException(
                "Follow request is already " + request.getStatus().name().toLowerCase());
        }
    }

    private FollowStatuses determineRelationshipStatus(Users viewer, Users targetUser) {
        if (usersRepository.isFollowing(viewer.getId(), targetUser.getId())) {
            return FollowStatuses.FOLLOWING;
        }
        if (hasPendingFollowRequest(viewer, targetUser)) {
            return FollowStatuses.REQUESTED;
        }
        return FollowStatuses.NOT_FOLLOWING;
    }

    @Transactional
    public FollowRequestResponseDTO respondToFollowRequest(Long requestId, Users user, FollowRequestStatuses response) {
        FollowRequest request = followRequestRepository.findById(Objects.requireNonNull(requestId, "requestId"))
            .orElseThrow(() -> new FollowRequestNotFoundException("Request not found"));

        if (response == FollowRequestStatuses.CANCELED) {
            if (!request.getRequestUser().getId().equals(user.getId())) {
                throw new UnauthorizedFollowRequestAccessException("You can only cancel your own requests");
            }
            validatePendingFollowRequestState(request);
            followRequestRepository.delete(request);
            log.info("Follow request canceled requestId={} username={} targetUsername={}",
                requestId,
                request.getRequestUser().getUsername(),
                request.getTargetUser().getUsername());
            return FollowRequestResponseDTO.builder()
            .newStatus(FollowRequestStatuses.CANCELED)
            .requestId(Objects.requireNonNull(requestId, "requestId"))
            .build();
        } else {
            if (!request.getTargetUser().getId().equals(user.getId())) {
                throw new UnauthorizedFollowRequestAccessException("Not your request");
            }
            validatePendingFollowRequestState(request);
            request.setStatus(response);
            request.setRespondedAt(LocalDateTime.now());

            if (response == FollowRequestStatuses.ACCEPTED) {
                insertFollowRelation(
                    request.getRequestUser().getId(),
                    request.getTargetUser().getId(),
                    new FollowRequestStateConflictException(
                        "Follow request cannot be accepted because the users are already following"));
            }

            followRequestRepository.save(request);
            log.info("Follow request responded requestId={} response={} requestUsername={} targetUsername={}",
                requestId,
                response,
                request.getRequestUser().getUsername(),
                request.getTargetUser().getUsername());
            return FollowRequestResponseDTO.builder()
                .requestId(requestId)
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
        Page<FollowRequest> page = followRequestRepository.findByTargetUserAndStatus(user, FollowRequestStatuses.PENDING, pageable);
        return page.map(watchMateMapper::mapToFollowRequestDTO);
    }

    @Transactional(readOnly = true)
    public Page<FollowRequestDTO> getSentRequests(Users user, int pageNumber, int size) {
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by("requestedAt").descending().and(Sort.by("id").descending()));
        return followRequestRepository.findByRequestUserAndStatus(user, FollowRequestStatuses.PENDING, pageable)
            .map(watchMateMapper::mapToFollowRequestDTO);
    }

    @Transactional
    public UpdateProfileResponseDTO updateProfile(Users user, UpdateProfileRequestDTO request) {
        user.setPrivacyStatus(request.getPrivacyStatus());
        Users saved = usersRepository.save(user);
        return UpdateProfileResponseDTO.builder()
            .userId(saved.getId())
            .privacyStatus(saved.getPrivacyStatus())
            .build();
    }

    @Transactional(readOnly = true)
    public FollowStatusDTO getFollowStatus(Long userId, Users user) {
        if (user.getId().equals(userId)){
            return FollowStatusDTO.builder()
           .followStatus(FollowStatuses.SELF)
           .build();
        }
        Users targetUser = findAndValidateTargetUser(userId);
        if (usersRepository.isEitherBlocking(user.getId(), targetUser.getId())) {
            return FollowStatusDTO.builder()
            .followStatus(FollowStatuses.BLOCKED)
            .build();
        }
        return FollowStatusDTO.builder()
        .followStatus(determineRelationshipStatus(user, targetUser))
        .build();
    }

    @Transactional
    public FollowStatusDTO blockUser(Long userId, Users user) {
        Users targetUser = findAndValidateTargetUser(userId);
        if (user.getId().equals(userId)){
            throw new SelfFollowException("Cannot block yourself!");
        }
        if (usersRepository.isBlockingUser(user.getId(), targetUser.getId())){
            return FollowStatusDTO.builder().followStatus(FollowStatuses.BLOCKED).build();
        }
        usersRepository.insertBlockRelation(user.getId(), targetUser.getId());
        usersRepository.deleteFollowRelation(user.getId(), targetUser.getId());
        usersRepository.deleteFollowRelation(targetUser.getId(), user.getId());
        followRequestRepository.deleteByRequestUserAndTargetUser(user, targetUser);
        followRequestRepository.deleteByRequestUserAndTargetUser(targetUser, user);
        log.info("User blocked target username={} targetUsername={}", user.getUsername(), targetUser.getUsername());
        return FollowStatusDTO.builder().followStatus(FollowStatuses.BLOCKED).build();
    }

    @Transactional
    public FollowStatusDTO unblockUser(Long userId, Users user) {
        Users targetUser = findAndValidateTargetUser(userId);
        if (user.getId().equals(userId)){
            throw new SelfFollowException("Cannot unblock yourself!");
        }
        if (!usersRepository.isBlockingUser(user.getId(), targetUser.getId())){
            return FollowStatusDTO.builder().followStatus(FollowStatuses.NOT_FOLLOWING).build();
        }
        usersRepository.deleteBlockRelation(user.getId(), targetUser.getId());
        log.info("User unblocked target username={} targetUsername={}", user.getUsername(), targetUser.getUsername());
        return FollowStatusDTO.builder().followStatus(FollowStatuses.NOT_FOLLOWING).build();
    }

    @Transactional(readOnly = true)
    public Page<FollowListUserDetailsDTO> getFollowersList(Users user, int pageNumber, int size) {
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by("username").ascending().and(Sort.by("id").descending()));
        Page<Users> page = usersRepository.findFollowersByUser(user, pageable);
        return page.map(watchMateMapper::mapToFollowListUserDetailsDTO);
    }

    @Transactional(readOnly = true)
    public Page<FollowListUserDetailsDTO> getFollowingList(Users user, int pageNumber, int size) {
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by("username").ascending().and(Sort.by("id").descending()));
        Page<Users> page = usersRepository.findFollowingByUser(user, pageable);
        return page.map(watchMateMapper::mapToFollowListUserDetailsDTO);
    }

    @Transactional(readOnly = true)
    public List<SearchListUserDetailsDTO> searchUsersByUsername(String query, Users user) {
        String normalizedQuery = Objects.requireNonNull(query, "query").trim();
        if (normalizedQuery.isBlank()) {
            throw new IllegalArgumentException("Query must not be blank");
        }
        Pageable limit = PageRequest.of(0, 15);
        return usersRepository.searchByUsername(normalizedQuery, user.getId(), limit);
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(Long userId, Users user) {
        Users targetUser = findAndValidateTargetUser(userId);
        return buildUserProfile(targetUser, user);
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(String username, Users user) {
        Users targetUser = findAndValidateTargetUser(username);
        return buildUserProfile(targetUser, user);
    }

    private UserProfileDTO buildUserProfile(Users targetUser, Users user) {
        if(user.getId().equals(targetUser.getId())){
            return retrieveSelfUserProfile(targetUser);
        }
        if(usersRepository.isEitherBlocking(targetUser.getId(), user.getId())){
            return UserProfileDTO.builder()
            .userId(targetUser.getId())
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
            .userId(user.getId())
            .username(user.getUsername())
            .privacyStatus(user.getPrivacyStatus())
            .followStatus(FollowStatuses.SELF)
            .followersCount(usersRepository.countFollowersByUserId(user.getId()))
            .followingCount(usersRepository.countFollowingByUserId(user.getId()))
            .watchlists(getProfileWatchLists(user, user))
            .reviews(reviewService.getReviewPage(user).map(review -> watchMateMapper.mapToReviewDTO(review)))
            .moviesWatchedCount(mediaService.countMoviesWatched(user))
            .showsWatchedCount(mediaService.countShowsWatched(user))
            .moviesWatched(mediaService.getMoviesWatchedPage(user).map(m -> watchMateMapper.mapToSearchItemDTO(m)))
            .showsWatched(mediaService.getShowsWatchedPage(user).map(m -> watchMateMapper.mapToSearchItemDTO(m)))
            .build();
    }

    private UserProfileDTO retrieveTargetUserProfile(Users user, Users targetUser){
        FollowStatuses relationshipStatus = determineRelationshipStatus(user, targetUser);
        if (relationshipStatus == FollowStatuses.FOLLOWING
                || targetUser.getPrivacyStatus() == PrivacyStatuses.PUBLIC
                || canViewPrivateProfile(user)){
            return UserProfileDTO.builder()
                .userId(targetUser.getId())
                .username(targetUser.getUsername())
                .privacyStatus(targetUser.getPrivacyStatus())
                .followStatus(relationshipStatus)
                .followersCount(usersRepository.countFollowersByUserId(targetUser.getId()))
                .followingCount(usersRepository.countFollowingByUserId(targetUser.getId()))
                .watchlists(getProfileWatchLists(targetUser, user))
                .moviesWatchedCount(mediaService.countMoviesWatched(targetUser))
                .showsWatchedCount(mediaService.countShowsWatched(targetUser))
                .build();
        } else {
            return UserProfileDTO.builder()
                .userId(targetUser.getId())
                .username(targetUser.getUsername())
                .privacyStatus(targetUser.getPrivacyStatus())
                .followStatus(relationshipStatus)
                .followersCount(usersRepository.countFollowersByUserId(targetUser.getId()))
                .followingCount(usersRepository.countFollowingByUserId(targetUser.getId()))
                .build();
        }
    }

    private boolean canViewPrivateProfile(Users user) {
        return user.getRole() == Role.MODERATOR || user.getRole() == Role.ADMIN;
    }

    /**
     * Asserts that {@code viewer} is permitted to access {@code targetUser}'s profile content.
     * Throws the appropriate typed exception when access is denied, matching the behavior of
     * {@link #buildUserProfile}:
     * <ul>
     *   <li>{@link BlockedUserException} (403) when either party has blocked the other</li>
     *   <li>{@link PrivateProfileException} (403) when target is PRIVATE and viewer is not a
     *       follower, MODERATOR, or ADMIN</li>
     * </ul>
     */
    private void assertProfileViewable(Users targetUser, Users viewer) {
        if (usersRepository.isEitherBlocking(targetUser.getId(), viewer.getId())) {
            throw new BlockedUserException("Profile is not accessible");
        }
        if (targetUser.getPrivacyStatus() == PrivacyStatuses.PRIVATE
                && !usersRepository.isFollowing(viewer.getId(), targetUser.getId())
                && !canViewPrivateProfile(viewer)) {
            throw new PrivateProfileException("This profile is private");
        }
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getUserReviews(Long targetUserId, Users viewer, int page, int size) {
        if (viewer.getId().equals(targetUserId)) {
            return reviewService.getReviewsByUser(viewer, page, size);
        }
        Users targetUser = findAndValidateTargetUser(targetUserId);
        assertProfileViewable(targetUser, viewer);
        return reviewService.getReviewsByUser(targetUser, page, size);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getUserReviews(String targetUsername, Users viewer, int page, int size) {
        Users targetUser = findAndValidateTargetUser(targetUsername);
        if (viewer.getId().equals(targetUser.getId())) {
            return reviewService.getReviewsByUser(viewer, page, size);
        }
        assertProfileViewable(targetUser, viewer);
        return reviewService.getReviewsByUser(targetUser, page, size);
    }

    private Page<WatchListDTO> getProfileWatchLists(Users profileOwner, Users viewer) {
        Page<WatchList> watchListPage = watchListService.getWatchListPage(profileOwner);
        return new PageImpl<>(
            watchListService.mapWatchListsForViewer(watchListPage.getContent(), viewer),
            watchListPage.getPageable(),
            watchListPage.getTotalElements()
        );
    }
}
