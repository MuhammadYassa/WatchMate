package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.project.watchmate.Models.UserPrincipal;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Repositories.UsersRepository;

@ExtendWith(MockitoExtension.class)
class WmUserDetailsServiceTest {

    @Mock
    private UsersRepository userRepo;

    @InjectMocks
    private WmUserDetailsService wmUserDetailsService;

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsernameTests {

        @Test
        void loadUserByUsername_WhenUserExistsAndVerified_ReturnsUserDetails() {
            Users user = Users.builder().id(1L).username("testuser").emailVerified(true).build();
            when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));

            UserDetails result = wmUserDetailsService.loadUserByUsername("testuser");

            assertNotNull(result);
            assertEquals("testuser", result.getUsername());
            assertTrue(result instanceof UserPrincipal);
        }

        @Test
        void loadUserByUsername_WhenUserNotFound_ThrowsUsernameNotFoundException() {
            when(userRepo.findByUsername("unknown")).thenReturn(Optional.empty());

            UsernameNotFoundException e = assertThrows(UsernameNotFoundException.class,
                () -> wmUserDetailsService.loadUserByUsername("unknown"));

            assertEquals("Username not found", e.getMessage());
        }

        @Test
        void loadUserByUsername_WhenEmailNotVerified_ThrowsDisabledException() {
            Users user = Users.builder().id(1L).username("testuser").emailVerified(false).build();
            when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));

            DisabledException e = assertThrows(DisabledException.class,
                () -> wmUserDetailsService.loadUserByUsername("testuser"));

            assertEquals("Email not verified", e.getMessage());
        }
    }
}
