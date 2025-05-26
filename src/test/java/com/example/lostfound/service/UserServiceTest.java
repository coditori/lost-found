package com.example.lostfound.service;

import com.example.lostfound.dto.UserDto;
import com.example.lostfound.entity.Role;
import com.example.lostfound.entity.User;
import com.example.lostfound.exception.UserNotFoundException;
import com.example.lostfound.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .name("Test User")
                .email("test@example.com")
                .role(Role.USER)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        adminUser = User.builder()
                .id(2L)
                .username("admin")
                .password("encodedAdminPassword")
                .name("Admin User")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void loadUserByUsername_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserDetails result = userService.loadUserByUsername("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getPassword()).isEqualTo("encodedPassword");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.isAccountNonExpired()).isTrue();
        assertThat(result.isAccountNonLocked()).isTrue();
        assertThat(result.isCredentialsNonExpired()).isTrue();

        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_USER");

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_AdminUser() {
        // Given
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        // When
        UserDetails result = userService.loadUserByUsername("admin");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getPassword()).isEqualTo("encodedAdminPassword");
        assertThat(result.isEnabled()).isTrue();

        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");

        verify(userRepository).findByUsername("admin");
    }

    @Test
    void loadUserByUsername_UserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found: nonexistent");

        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void loadUserByUsername_DisabledUser() {
        // Given
        testUser.setEnabled(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserDetails result = userService.loadUserByUsername("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.isEnabled()).isFalse();

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_ExpiredAccount() {
        // Given
        testUser.setAccountNonExpired(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserDetails result = userService.loadUserByUsername("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.isAccountNonExpired()).isFalse();

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_LockedAccount() {
        // Given
        testUser.setAccountNonLocked(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserDetails result = userService.loadUserByUsername("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.isAccountNonLocked()).isFalse();

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_ExpiredCredentials() {
        // Given
        testUser.setCredentialsNonExpired(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserDetails result = userService.loadUserByUsername("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.isCredentialsNonExpired()).isFalse();

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getUserById_Success() throws UserNotFoundException {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserDto result = userService.getUserById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getName()).isEqualTo("Test User");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getRole()).isEqualTo(Role.USER);
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getCreatedAt()).isEqualTo(testUser.getCreatedAt());

        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_UserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: 999");

        verify(userRepository).findById(999L);
    }

    @Test
    void getUserById_AdminUser() throws UserNotFoundException {
        // Given
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));

        // When
        UserDto result = userService.getUserById(2L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getName()).isEqualTo("Admin User");
        assertThat(result.getEmail()).isEqualTo("admin@example.com");
        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
        assertThat(result.isEnabled()).isTrue();

        verify(userRepository).findById(2L);
    }

    @Test
    void getUserByUsername_Success() throws UserNotFoundException {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserDto result = userService.getUserByUsername("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getName()).isEqualTo("Test User");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getRole()).isEqualTo(Role.USER);
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getCreatedAt()).isEqualTo(testUser.getCreatedAt());

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getUserByUsername_UserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserByUsername("nonexistent"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: nonexistent");

        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void getUserByUsername_AdminUser() throws UserNotFoundException {
        // Given
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        // When
        UserDto result = userService.getUserByUsername("admin");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getName()).isEqualTo("Admin User");
        assertThat(result.getEmail()).isEqualTo("admin@example.com");
        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
        assertThat(result.isEnabled()).isTrue();

        verify(userRepository).findByUsername("admin");
    }

    @Test
    void getUserByUsername_DisabledUser() throws UserNotFoundException {
        // Given
        testUser.setEnabled(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserDto result = userService.getUserByUsername("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.isEnabled()).isFalse();

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_EmptyUsername() {
        // Given
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.loadUserByUsername(""))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found: ");

        verify(userRepository).findByUsername("");
    }

    @Test
    void getUserById_NullId() {
        // Given
        when(userRepository.findById(null)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(null))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: null");

        verify(userRepository).findById(null);
    }

    @Test
    void getUserByUsername_NullUsername() {
        // Given
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserByUsername(null))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: null");

        verify(userRepository).findByUsername(null);
    }

    @Test
    void convertToDto_AllFields() throws UserNotFoundException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        testUser.setCreatedAt(now);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserDto result = userService.getUserById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUser.getId());
        assertThat(result.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(result.getName()).isEqualTo(testUser.getName());
        assertThat(result.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(result.getRole()).isEqualTo(testUser.getRole());
        assertThat(result.getCreatedAt()).isEqualTo(testUser.getCreatedAt());
        assertThat(result.isEnabled()).isEqualTo(testUser.isEnabled());

        verify(userRepository).findById(1L);
    }
} 