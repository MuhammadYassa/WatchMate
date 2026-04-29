package com.project.watchmate.Services;

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
import org.springframework.data.domain.Pageable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.Dto.FollowRequestResponseDTO;
import com.project.watchmate.Dto.FollowStatusDTO;
import com.project.watchmate.Dto.SearchListUserDetailsDTO;
import com.project.watchmate.Dto.UserProfileDTO;
import com.project.watchmate.Exception.AlreadyFollowingException;
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
    private UserMediaStatusRepository userMediaStatusRepository;

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
            when(usersRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetUser));
            when(usersRepository.isFollowing(USER_ID, TARGET_ID)).thenReturn(false);
            when(usersRepository.isBlockedByUser(TARGET_ID, USER_ID)).thenReturn(false);
            when(usersRepository.isBlockingUser(USER_ID, TARGET_ID)).thenReturn(false);
            
            FollowStatusDTO result = socialService.followUser(TARGET_ID, user);

            assertEquals(FollowStatuses.FOLLOWING, result.getFollowStatus());
            verify(usersRepository).insertFollowRelation(USER_ID, TARGET_ID);
        }

        @Test
        void followUser_WhenUserNotFound_ThrowsUserNotFoundException() {
            when(usersRepository.findById(TARGET_ID)).thenReturn(Optional.empty());

            UserNotFoundException e = assertThrows(UserNotFoundException.class, () -> socialService.followUser(TARGET_ID, user));

            assertEquals("User not found", e.getMessage());
            verify(usersRepository).findById(TARGET_ID);
        }

        @Test
        void followUser_WhenSelfFollow_ThrowsSelfFollowException() {
            when(usersRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            SelfFollowException e = assertThrows(SelfFollowException.class, () -> socialService.followUser(USER_ID, user));

            assertEquals("You cannot follow yourself!", e.getMessage());
        }

        @Test
        void followUser_WhenAlreadyFollowing_ThrowsAlreadyFollowingException() {
            when(usersRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetUser));
            when(usersRepository.isFollowing(USER_ID, TARGET_ID)).thenReturn(true);

            AlreadyFollowingException e = assertThrows(AlreadyFollowingException.class, () -> socialService.followUser(TARGET_ID, user));

            assertEquals("User already following!", e.getMessage());
            verify(usersRepository, never()).save(any(Users.class));
        }
    }

    @Nested
    @DisplayName("Unfollow User Tests")
    class UnfollowUserTests {

        @Test
        void unfollowUser_WhenFollowing_RemovesAndSaves() {
            when(usersRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetUser));
            when(usersRepository.isFollowing(USER_ID, TARGET_ID)).thenReturn(true);
            when(usersRepository.isBlockedByUser(TARGET_ID, USER_ID)).thenReturn(false);
            when(usersRepository.isBlockingUser(USER_ID, TARGET_ID)).thenReturn(false);

            FollowStatusDTO result = socialService.unfollowUser(TARGET_ID, user);

            assertEquals(FollowStatuses.NOT_FOLLOWING, result.getFollowStatus());
            verify(usersRepository).deleteFollowRelation(USER_ID, TARGET_ID);
        }

        @Test
        void unfollowUser_WhenNotFollowing_ThrowsNotFollowingException() {
            when(usersRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetUser));
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
            when(usersRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetUser));

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
            when(usersRepository.findById(USER_ID)).thenReturn(Optional.of(user));

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
            when(usersRepository.findById(TARGET_ID)).thenReturn(Optional.of(targetUser));
            when(usersRepository.isBlockedByUser(USER_ID, TARGET_ID)).thenReturn(false);
            when(usersRepository.isBlockingUser(TARGET_ID, USER_ID)).thenReturn(false);
            when(usersRepository.isFollowing(USER_ID, TARGET_ID)).thenReturn(true);

            FollowStatusDTO result = socialService.getFollowStatus(TARGET_ID, user);

            assertEquals(FollowStatuses.FOLLOWING, result.getFollowStatus());
        }

        @Test
        void getFollowStatus_WhenTargetNotFound_ThrowsUserNotFoundException() {
            when(usersRepository.findById(999L)).thenReturn(Optional.empty());

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
            when(userMediaStatusRepository.countWatchedMoviesByUser(targetUser)).thenReturn(0L);
            when(userMediaStatusRepository.countWatchedShowsByUser(targetUser)).thenReturn(0L);
            when(watchListService.getWatchListPage(targetUser)).thenReturn(Page.empty());

            UserProfileDTO result = socialService.getUserProfile("target", user);

            assertEquals("target", result.getUsername());
            assertEquals(PrivacyStatuses.PUBLIC, result.getPrivacyStatus());
        }
    }
}
