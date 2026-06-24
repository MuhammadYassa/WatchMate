package com.project.watchmate.social.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.social.dto.FollowRequestResponseDTO;
import com.project.watchmate.social.dto.FollowStatusDTO;
import com.project.watchmate.social.dto.SearchListUserDetailsDTO;
import com.project.watchmate.social.dto.UserProfileDTO;
import com.project.watchmate.common.error.AlreadyFollowingException;
import com.project.watchmate.common.error.FollowRequestNotFoundException;
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
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.social.persistence.FollowRequestRepository;
import com.project.watchmate.user.persistence.UsersRepository;
import com.project.watchmate.watchlist.application.WatchListService;
import com.project.watchmate.watchlist.domain.WatchList;
import com.project.watchmate.watchlist.dto.WatchListDTO;

@ExtendWith(MockitoExtension.class)
class SocialServiceTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private FollowRequestRepository followRequestRepository;

    @Mock
    private WatchMateMapper watchMateMapper;

    @Mock
    private WatchListService watchListService;

    @Mock
    private MediaService mediaService;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private SocialService socialService;

    private Users user;
    private Users targetUser;
    private static final Long USER_ID = 1L;
    private static final Long TARGET_ID = 2L;


    @BeforeEach
    void setUp() {
        user = Users.builder().id(USER_ID).username("user").following(new ArrayList<>()).followers(new ArrayList<>()).blockedUsers(new ArrayList<>()).build();
        targetUser = Users.builder().id(TARGET_ID).username("target").following(new ArrayList<>()).followers(new ArrayList<>()).privacyStatus(PrivacyStatuses.PUBLIC).build();
    }

    @Nested
    @DisplayName("Follow User Tests")
    class FollowUserTests {

        @Test
        void followUser_WhenPublicTarget_PerformsDirectFollowAndSaves() {
            when(usersRepository.findByIdAndEmailVerifiedTrue(TARGET_ID)).thenReturn(Optional.of(targetUser));
            when(usersRepository.isFollowing(USER_ID, TARGET_ID)).thenReturn(false);
            when(usersRepository.isBlockingUser(TARGET_ID, USER_ID)).thenReturn(false);
            when(usersRepository.isBlockingUser(USER_ID, TARGET_ID)).thenReturn(false);

            FollowStatusDTO result = socialService.followUser(TARGET_ID, user);

            assertEquals(FollowStatuses.FOLLOWING, result.getFollowStatus());
            verify(usersRepository).insertFollowRelation(USER_ID, TARGET_ID);
        }

        @Test
        void followUser_WhenUserNotFound_ThrowsUserNotFoundException() {
            when(usersRepository.findByIdAndEmailVerifiedTrue(TARGET_ID)).thenReturn(Optional.empty());

            UserNotFoundException e = assertThrows(UserNotFoundException.class, () -> socialService.followUser(TARGET_ID, user));

            assertEquals("User not found", e.getMessage());
            verify(usersRepository).findByIdAndEmailVerifiedTrue(TARGET_ID);
        }

        @Test
        void followUser_WhenSelfFollow_ThrowsSelfFollowException() {
            when(usersRepository.findByIdAndEmailVerifiedTrue(USER_ID)).thenReturn(Optional.of(user));

            SelfFollowException e = assertThrows(SelfFollowException.class, () -> socialService.followUser(USER_ID, user));

            assertEquals("You cannot follow yourself!", e.getMessage());
        }

        @Test
        void followUser_WhenAlreadyFollowing_ThrowsAlreadyFollowingException() {
            when(usersRepository.findByIdAndEmailVerifiedTrue(TARGET_ID)).thenReturn(Optional.of(targetUser));
            when(usersRepository.isFollowing(USER_ID, TARGET_ID)).thenReturn(true);

            AlreadyFollowingException e = assertThrows(AlreadyFollowingException.class, () -> socialService.followUser(TARGET_ID, user));

            assertEquals("User already following!", e.getMessage());
            verify(usersRepository, never()).save(any(Users.class));
        }

        @Test
        void followUser_WhenPrivateTarget_ReturnsRequested() {
            targetUser.setPrivacyStatus(PrivacyStatuses.PRIVATE);
            when(usersRepository.findByIdAndEmailVerifiedTrue(TARGET_ID)).thenReturn(Optional.of(targetUser));
            when(usersRepository.isFollowing(USER_ID, TARGET_ID)).thenReturn(false);
            when(usersRepository.isBlockingUser(TARGET_ID, USER_ID)).thenReturn(false);
            when(usersRepository.isBlockingUser(USER_ID, TARGET_ID)).thenReturn(false);
            when(followRequestRepository.existsByRequestUserAndTargetUserAndStatus(user, targetUser, FollowRequestStatuses.PENDING))
                .thenReturn(false);

            FollowStatusDTO result = socialService.followUser(TARGET_ID, user);

            assertEquals(FollowStatuses.REQUESTED, result.getFollowStatus());
            verify(followRequestRepository).save(any(FollowRequest.class));
        }
    }

    @Nested
    @DisplayName("Unfollow User Tests")
    class UnfollowUserTests {

        @Test
        void unfollowUser_WhenFollowing_RemovesAndSaves() {
            when(usersRepository.findByIdAndEmailVerifiedTrue(TARGET_ID)).thenReturn(Optional.of(targetUser));
            when(usersRepository.isFollowing(USER_ID, TARGET_ID)).thenReturn(true);
            when(usersRepository.isBlockingUser(TARGET_ID, USER_ID)).thenReturn(false);
            when(usersRepository.isBlockingUser(USER_ID, TARGET_ID)).thenReturn(false);

            FollowStatusDTO result = socialService.unfollowUser(TARGET_ID, user);

            assertEquals(FollowStatuses.NOT_FOLLOWING, result.getFollowStatus());
            verify(usersRepository).deleteFollowRelation(USER_ID, TARGET_ID);
        }

        @Test
        void unfollowUser_WhenNotFollowing_ThrowsNotFollowingException() {
            when(usersRepository.findByIdAndEmailVerifiedTrue(TARGET_ID)).thenReturn(Optional.of(targetUser));
            when(usersRepository.isFollowing(USER_ID, TARGET_ID)).thenReturn(false);

            NotFollowingException e = assertThrows(NotFollowingException.class, () -> socialService.unfollowUser(TARGET_ID, user));

            assertEquals("Not following target user!", e.getMessage());
        }
    }

    @Nested
    @DisplayName("Respond to Follow Request Tests")
    class RespondToFollowRequestTests {

        @Test
        void respondToFollowRequest_WhenTargetAccepts_PerformsFollowAndSaves() {
            Long requestId = 10L;
            FollowRequest request = FollowRequest.builder()
                .id(requestId)
                .requestUser(user)
                .targetUser(targetUser)
                .status(FollowRequestStatuses.PENDING)
                .build();
            when(followRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

            FollowRequestResponseDTO result = socialService.respondToFollowRequest(requestId, targetUser, FollowRequestStatuses.ACCEPTED);

            assertEquals(FollowRequestStatuses.ACCEPTED, result.getNewStatus());
            verify(usersRepository).insertFollowRelation(USER_ID, TARGET_ID);
            verify(followRequestRepository).save(request);
        }

        @Test
        void respondToFollowRequest_WhenAcceptingNonPendingRequest_ThrowsConflictWithoutFollowingInsert() {
            Long requestId = 10L;
            FollowRequest request = FollowRequest.builder()
                .id(requestId)
                .requestUser(user)
                .targetUser(targetUser)
                .status(FollowRequestStatuses.ACCEPTED)
                .build();
            when(followRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

            FollowRequestStateConflictException e = assertThrows(FollowRequestStateConflictException.class,
                () -> socialService.respondToFollowRequest(requestId, targetUser, FollowRequestStatuses.ACCEPTED));

            assertEquals("Follow request is already accepted", e.getMessage());
            verify(usersRepository, never()).insertFollowRelation(any(), any());
            verify(followRequestRepository, never()).save(any(FollowRequest.class));
        }

        @Test
        void respondToFollowRequest_WhenRejectingNonPendingRequest_ThrowsConflict() {
            Long requestId = 10L;
            FollowRequest request = FollowRequest.builder()
                .id(requestId)
                .requestUser(user)
                .targetUser(targetUser)
                .status(FollowRequestStatuses.REJECTED)
                .build();
            when(followRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

            FollowRequestStateConflictException e = assertThrows(FollowRequestStateConflictException.class,
                () -> socialService.respondToFollowRequest(requestId, targetUser, FollowRequestStatuses.REJECTED));

            assertEquals("Follow request is already rejected", e.getMessage());
            verify(followRequestRepository, never()).save(any(FollowRequest.class));
        }

        @Test
        void respondToFollowRequest_WhenRequesterCancelsPendingRequest_DeletesIt() {
            Long requestId = 10L;
            FollowRequest request = FollowRequest.builder()
                .id(requestId)
                .requestUser(user)
                .targetUser(targetUser)
                .status(FollowRequestStatuses.PENDING)
                .build();
            when(followRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

            FollowRequestResponseDTO result = socialService.respondToFollowRequest(requestId, user, FollowRequestStatuses.CANCELED);

            assertEquals(FollowRequestStatuses.CANCELED, result.getNewStatus());
            verify(followRequestRepository).delete(request);
            verify(followRequestRepository, never()).save(any(FollowRequest.class));
        }

        @Test
        void respondToFollowRequest_WhenCancelingNonPendingRequest_ThrowsConflict() {
            Long requestId = 10L;
            FollowRequest request = FollowRequest.builder()
                .id(requestId)
                .requestUser(user)
                .targetUser(targetUser)
                .status(FollowRequestStatuses.ACCEPTED)
                .build();
            when(followRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

            FollowRequestStateConflictException e = assertThrows(FollowRequestStateConflictException.class,
                () -> socialService.respondToFollowRequest(requestId, user, FollowRequestStatuses.CANCELED));

            assertEquals("Follow request is already accepted", e.getMessage());
            verify(followRequestRepository, never()).delete(any(FollowRequest.class));
        }

        @Test
        void respondToFollowRequest_WhenNotTargetUser_ThrowsUnauthorized() {
            Long requestId = 10L;
            Users other = Users.builder().id(99L).username("other").build();
            FollowRequest request = FollowRequest.builder().id(requestId).requestUser(user).targetUser(targetUser).status(FollowRequestStatuses.PENDING).build();
            when(followRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

            UnauthorizedFollowRequestAccessException e = assertThrows(UnauthorizedFollowRequestAccessException.class,
                () -> socialService.respondToFollowRequest(requestId, other, FollowRequestStatuses.ACCEPTED));
            assertEquals("Not your request", e.getMessage());
        }

        @Test
        void respondToFollowRequest_WhenCancelingAnotherUsersRequest_ThrowsUnauthorized() {
            Long requestId = 10L;
            Users other = Users.builder().id(99L).username("other").build();
            FollowRequest request = FollowRequest.builder().id(requestId).requestUser(user).targetUser(targetUser).status(FollowRequestStatuses.PENDING).build();
            when(followRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

            UnauthorizedFollowRequestAccessException e = assertThrows(UnauthorizedFollowRequestAccessException.class,
                () -> socialService.respondToFollowRequest(requestId, other, FollowRequestStatuses.CANCELED));
            assertEquals("You can only cancel your own requests", e.getMessage());
        }

        @Test
        void respondToFollowRequest_WhenRequestNotFound_ThrowsFollowRequestNotFoundException() {
            when(followRequestRepository.findById(999L)).thenReturn(Optional.empty());

            FollowRequestNotFoundException e = assertThrows(FollowRequestNotFoundException.class,
                () -> socialService.respondToFollowRequest(999L, user, FollowRequestStatuses.ACCEPTED));

            assertEquals("Request not found", e.getMessage());
        }
    }

    @Nested
    @DisplayName("Block User Tests")
    class BlockUserTests {

        @Test
        void blockUser_WhenNotAlreadyBlocking_AddsToBlockedAndReturnsBlockedStatus() {
            when(usersRepository.findByIdAndEmailVerifiedTrue(TARGET_ID)).thenReturn(Optional.of(targetUser));

            FollowStatusDTO result = socialService.blockUser(TARGET_ID, user);

            assertEquals(FollowStatuses.BLOCKED, result.getFollowStatus());
            verify(usersRepository).insertBlockRelation(USER_ID, TARGET_ID);
            verify(usersRepository).deleteFollowRelation(USER_ID, TARGET_ID);
            verify(usersRepository).deleteFollowRelation(TARGET_ID, USER_ID);
            verify(followRequestRepository).deleteByRequestUserAndTargetUser(user, targetUser);
            verify(followRequestRepository).deleteByRequestUserAndTargetUser(targetUser, user);
        }

        @Test
        void blockUser_WhenBlockingSelf_ThrowsSelfFollowException() {
            when(usersRepository.findByIdAndEmailVerifiedTrue(USER_ID)).thenReturn(Optional.of(user));

            SelfFollowException e = assertThrows(SelfFollowException.class, () -> socialService.blockUser(USER_ID, user));
            assertEquals("Cannot block yourself!", e.getMessage());
        }
    }

    @Nested
    @DisplayName("Get Follow Status Tests")
    class GetFollowStatusTests {

        @Test
        void getFollowStatus_WhenSelf_ReturnsNotFollowing() {
            FollowStatusDTO result = socialService.getFollowStatus(USER_ID, user);

            assertEquals(FollowStatuses.NOT_FOLLOWING, result.getFollowStatus());
        }

        @Test
        void getFollowStatus_WhenFollowing_ReturnsFollowing() {
            when(usersRepository.findByIdAndEmailVerifiedTrue(TARGET_ID)).thenReturn(Optional.of(targetUser));
            when(usersRepository.isBlockingUser(USER_ID, TARGET_ID)).thenReturn(false);
            when(usersRepository.isBlockingUser(TARGET_ID, USER_ID)).thenReturn(false);
            when(usersRepository.isFollowing(USER_ID, TARGET_ID)).thenReturn(true);

            FollowStatusDTO result = socialService.getFollowStatus(TARGET_ID, user);

            assertEquals(FollowStatuses.FOLLOWING, result.getFollowStatus());
        }

        @Test
        void getFollowStatus_WhenPendingRequestExists_ReturnsRequested() {
            targetUser.setPrivacyStatus(PrivacyStatuses.PRIVATE);
            when(usersRepository.findByIdAndEmailVerifiedTrue(TARGET_ID)).thenReturn(Optional.of(targetUser));
            when(usersRepository.isBlockingUser(USER_ID, TARGET_ID)).thenReturn(false);
            when(usersRepository.isBlockingUser(TARGET_ID, USER_ID)).thenReturn(false);
            when(usersRepository.isFollowing(USER_ID, TARGET_ID)).thenReturn(false);
            when(followRequestRepository.existsByRequestUserAndTargetUserAndStatus(user, targetUser, FollowRequestStatuses.PENDING))
                .thenReturn(true);

            FollowStatusDTO result = socialService.getFollowStatus(TARGET_ID, user);

            assertEquals(FollowStatuses.REQUESTED, result.getFollowStatus());
        }

        @Test
        void getFollowStatus_WhenTargetNotFound_ThrowsUserNotFoundException() {
            when(usersRepository.findByIdAndEmailVerifiedTrue(999L)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> socialService.getFollowStatus(999L, user));
        }
    }

    @Nested
    @DisplayName("Username Lookup Tests")
    class UsernameLookupTests {

        @Test
        void searchUsersByUsername_TrimsQueryAndReturnsSearchDtos() {
            SearchListUserDetailsDTO exactDto = SearchListUserDetailsDTO.builder()
                .username("muh")
                .isFollowing(false)
                .isSelf(false)
                .privacyStatus(PrivacyStatuses.PUBLIC)
                .build();
            SearchListUserDetailsDTO prefixDto = SearchListUserDetailsDTO.builder()
                .username("muha")
                .isFollowing(true)
                .isSelf(false)
                .privacyStatus(PrivacyStatuses.PUBLIC)
                .build();

            when(usersRepository.searchByUsername(eq("muh"), eq(USER_ID), any(Pageable.class))).thenReturn(List.of(exactDto, prefixDto));

            List<SearchListUserDetailsDTO> result = socialService.searchUsersByUsername("  muh  ", user);

            assertEquals(List.of(exactDto, prefixDto), result);
            verify(usersRepository).searchByUsername(eq("muh"), eq(USER_ID), any(Pageable.class));
        }

        @Test
        void getUserProfileByUsername_WhenUserNotFound_ThrowsUserNotFoundException() {
            when(usersRepository.findByUsernameIgnoreCaseAndEmailVerifiedTrue("missing-user")).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> socialService.getUserProfile("missing-user", user));
        }

        @Test
        void getUserProfileByUsername_WhenUserExists_ReturnsProfile() {
            when(usersRepository.findByUsernameIgnoreCaseAndEmailVerifiedTrue("target")).thenReturn(Optional.of(targetUser));
            when(usersRepository.isBlockingUser(TARGET_ID, USER_ID)).thenReturn(false);
            when(usersRepository.isBlockingUser(USER_ID, TARGET_ID)).thenReturn(false);
            when(usersRepository.isFollowing(USER_ID, TARGET_ID)).thenReturn(false);
            when(usersRepository.countFollowersByUserId(TARGET_ID)).thenReturn(0L);
            when(usersRepository.countFollowingByUserId(TARGET_ID)).thenReturn(0L);
            when(mediaService.countMoviesWatched(targetUser)).thenReturn(0L);
            when(mediaService.countShowsWatched(targetUser)).thenReturn(0L);
            when(watchListService.getWatchListPage(targetUser)).thenReturn(Page.empty());
            when(watchListService.mapWatchListsForViewer(List.of(), user)).thenReturn(List.of());

            UserProfileDTO result = socialService.getUserProfile("target", user);

            assertEquals(TARGET_ID, result.getUserId());
            assertEquals("target", result.getUsername());
            assertEquals(PrivacyStatuses.PUBLIC, result.getPrivacyStatus());
            assertEquals(FollowStatuses.NOT_FOLLOWING, result.getFollowStatus());
        }

        @Test
        void getUserProfileByUsername_WhenViewerFollowsVisibleTarget_ReturnsFollowing() {
            when(usersRepository.findByUsernameIgnoreCaseAndEmailVerifiedTrue("target")).thenReturn(Optional.of(targetUser));
            when(usersRepository.isBlockingUser(TARGET_ID, USER_ID)).thenReturn(false);
            when(usersRepository.isBlockingUser(USER_ID, TARGET_ID)).thenReturn(false);
            when(usersRepository.isFollowing(USER_ID, TARGET_ID)).thenReturn(true);
            when(usersRepository.countFollowersByUserId(TARGET_ID)).thenReturn(1L);
            when(usersRepository.countFollowingByUserId(TARGET_ID)).thenReturn(0L);
            when(mediaService.countMoviesWatched(targetUser)).thenReturn(0L);
            when(mediaService.countShowsWatched(targetUser)).thenReturn(0L);
            when(watchListService.getWatchListPage(targetUser)).thenReturn(Page.empty());
            when(watchListService.mapWatchListsForViewer(List.of(), user)).thenReturn(List.of());

            UserProfileDTO result = socialService.getUserProfile("target", user);

            assertEquals(FollowStatuses.FOLLOWING, result.getFollowStatus());
        }

        @Test
        void getUserProfileByUsername_WhenPrivateProfileNotVisibleAndRequestPending_ReturnsRequested() {
            targetUser.setPrivacyStatus(PrivacyStatuses.PRIVATE);
            when(usersRepository.findByUsernameIgnoreCaseAndEmailVerifiedTrue("target")).thenReturn(Optional.of(targetUser));
            when(usersRepository.isBlockingUser(TARGET_ID, USER_ID)).thenReturn(false);
            when(usersRepository.isBlockingUser(USER_ID, TARGET_ID)).thenReturn(false);
            when(usersRepository.isFollowing(USER_ID, TARGET_ID)).thenReturn(false);
            when(followRequestRepository.existsByRequestUserAndTargetUserAndStatus(user, targetUser, FollowRequestStatuses.PENDING))
                .thenReturn(true);
            when(usersRepository.countFollowersByUserId(TARGET_ID)).thenReturn(0L);
            when(usersRepository.countFollowingByUserId(TARGET_ID)).thenReturn(0L);

            UserProfileDTO result = socialService.getUserProfile("target", user);

            assertEquals(TARGET_ID, result.getUserId());
            assertEquals(FollowStatuses.REQUESTED, result.getFollowStatus());
        }

        @Test
        void getUserProfileByUsername_WhenVisibleTarget_MapsWatchlistsUsingViewer() {
            WatchList watchList = WatchList.builder().id(15L).name("Owner List").user(targetUser).build();
            WatchListDTO dto = WatchListDTO.builder().id(15L).name("Owner List").media(List.of()).build();
            Page<WatchList> watchListPage = new PageImpl<>(List.of(watchList));

            when(usersRepository.findByUsernameIgnoreCaseAndEmailVerifiedTrue("target")).thenReturn(Optional.of(targetUser));
            when(usersRepository.isBlockingUser(TARGET_ID, USER_ID)).thenReturn(false);
            when(usersRepository.isBlockingUser(USER_ID, TARGET_ID)).thenReturn(false);
            when(usersRepository.isFollowing(USER_ID, TARGET_ID)).thenReturn(false);
            when(usersRepository.countFollowersByUserId(TARGET_ID)).thenReturn(0L);
            when(usersRepository.countFollowingByUserId(TARGET_ID)).thenReturn(0L);
            when(mediaService.countMoviesWatched(targetUser)).thenReturn(0L);
            when(mediaService.countShowsWatched(targetUser)).thenReturn(0L);
            when(watchListService.getWatchListPage(targetUser)).thenReturn(watchListPage);
            when(watchListService.mapWatchListsForViewer(List.of(watchList), user)).thenReturn(List.of(dto));

            UserProfileDTO result = socialService.getUserProfile("target", user);

            assertEquals(1, result.getWatchlists().getContent().size());
            verify(watchListService).mapWatchListsForViewer(List.of(watchList), user);
        }

        @Test
        void getUserProfileByUsername_WhenSelf_MapsWatchlistsUsingSelfAsViewer() {
            WatchList watchList = WatchList.builder().id(22L).name("Self List").user(user).build();
            WatchListDTO dto = WatchListDTO.builder().id(22L).name("Self List").media(List.of()).build();
            Page<WatchList> watchListPage = new PageImpl<>(List.of(watchList));

            when(usersRepository.findByUsernameIgnoreCaseAndEmailVerifiedTrue("user")).thenReturn(Optional.of(user));
            when(usersRepository.countFollowersByUserId(USER_ID)).thenReturn(0L);
            when(usersRepository.countFollowingByUserId(USER_ID)).thenReturn(0L);
            when(mediaService.countMoviesWatched(user)).thenReturn(0L);
            when(mediaService.countShowsWatched(user)).thenReturn(0L);
            when(reviewService.getReviewPage(user)).thenReturn(Page.empty());
            when(mediaService.getMoviesWatchedPage(user)).thenReturn(Page.empty());
            when(mediaService.getShowsWatchedPage(user)).thenReturn(Page.empty());
            when(watchListService.getWatchListPage(user)).thenReturn(watchListPage);
            when(watchListService.mapWatchListsForViewer(List.of(watchList), user)).thenReturn(List.of(dto));

            UserProfileDTO result = socialService.getUserProfile("user", user);

            assertEquals(USER_ID, result.getUserId());
            assertEquals(1, result.getWatchlists().getContent().size());
            verify(watchListService).mapWatchListsForViewer(List.of(watchList), user);
        }
    }
}




