package com.example.lostfound.service;

import com.example.lostfound.dto.ClaimDto;
import com.example.lostfound.dto.ClaimRequest;
import com.example.lostfound.entity.Claim;
import com.example.lostfound.entity.ClaimStatus;
import com.example.lostfound.entity.LostItem;
import com.example.lostfound.entity.Role;
import com.example.lostfound.entity.User;
import com.example.lostfound.exception.InsufficientQuantityException;
import com.example.lostfound.exception.LostItemNotFoundException;
import com.example.lostfound.exception.UserNotFoundException;
import com.example.lostfound.repository.ClaimRepository;
import com.example.lostfound.repository.LostItemRepository;
import com.example.lostfound.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private LostItemRepository lostItemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClaimService claimService;

    private User testUser;
    private LostItem testLostItem;
    private ClaimRequest testClaimRequest;
    private Claim testClaim;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .name("Test User")
                .email("test@example.com")
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        // Create test lost item
        testLostItem = LostItem.builder()
                .id(1L)
                .itemName("Test Laptop")
                .quantity(5)
                .remainingQuantity(3)
                .place("Library")
                .description("Test description")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(1L)
                .build();

        // Create test claim request
        testClaimRequest = new ClaimRequest(1L, 2, "I need this laptop for work");

        // Create test claim
        testClaim = Claim.builder()
                .id(1L)
                .user(testUser)
                .lostItem(testLostItem)
                .claimedQuantity(2)
                .status(ClaimStatus.PENDING)
                .notes("I need this laptop for work")
                .claimDate(LocalDateTime.now())
                .build();
    }

    @Test
    void createClaim_Success() throws Exception {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(lostItemRepository.findById(1L)).thenReturn(Optional.of(testLostItem));
        when(claimRepository.existsByUserIdAndLostItemId(1L, 1L)).thenReturn(false);
        when(lostItemRepository.save(any(LostItem.class))).thenReturn(testLostItem);
        when(claimRepository.save(any(Claim.class))).thenReturn(testClaim);

        // When
        ClaimDto result = claimService.createClaim(testClaimRequest, "testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getUserName()).isEqualTo("Test User");
        assertThat(result.getLostItemId()).isEqualTo(1L);
        assertThat(result.getItemName()).isEqualTo("Test Laptop");
        assertThat(result.getPlace()).isEqualTo("Library");
        assertThat(result.getClaimedQuantity()).isEqualTo(2);
        assertThat(result.getStatus()).isEqualTo(ClaimStatus.PENDING);
        assertThat(result.getNotes()).isEqualTo("I need this laptop for work");

        verify(userRepository).findByUsername("testuser");
        verify(lostItemRepository).findById(1L);
        verify(claimRepository).existsByUserIdAndLostItemId(1L, 1L);
        verify(lostItemRepository).save(testLostItem);
        verify(claimRepository).save(any(Claim.class));

        // Verify that the remaining quantity was decreased
        assertThat(testLostItem.getRemainingQuantity()).isEqualTo(1);
    }

    @Test
    void createClaim_UserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> claimService.createClaim(testClaimRequest, "nonexistent"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: nonexistent");

        verify(userRepository).findByUsername("nonexistent");
        verifyNoInteractions(lostItemRepository, claimRepository);
    }

    @Test
    void createClaim_LostItemNotFound() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(lostItemRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> claimService.createClaim(testClaimRequest, "testuser"))
                .isInstanceOf(LostItemNotFoundException.class)
                .hasMessage("Lost item not found with id: 1");

        verify(userRepository).findByUsername("testuser");
        verify(lostItemRepository).findById(1L);
        verifyNoInteractions(claimRepository);
    }

    @Test
    void createClaim_UserAlreadyClaimed() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(lostItemRepository.findById(1L)).thenReturn(Optional.of(testLostItem));
        when(claimRepository.existsByUserIdAndLostItemId(1L, 1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> claimService.createClaim(testClaimRequest, "testuser"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User has already claimed this item");

        verify(userRepository).findByUsername("testuser");
        verify(lostItemRepository).findById(1L);
        verify(claimRepository).existsByUserIdAndLostItemId(1L, 1L);
        verify(lostItemRepository, never()).save(any());
        verify(claimRepository, never()).save(any());
    }

    @Test
    void createClaim_InsufficientQuantity() {
        // Given
        ClaimRequest largeClaimRequest = new ClaimRequest(1L, 5, "Need all items");
        testLostItem.setRemainingQuantity(2); // Less than requested

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(lostItemRepository.findById(1L)).thenReturn(Optional.of(testLostItem));
        when(claimRepository.existsByUserIdAndLostItemId(1L, 1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> claimService.createClaim(largeClaimRequest, "testuser"))
                .isInstanceOf(InsufficientQuantityException.class)
                .hasMessage("Insufficient quantity. Requested: 5, Available: 2");

        verify(userRepository).findByUsername("testuser");
        verify(lostItemRepository).findById(1L);
        verify(claimRepository).existsByUserIdAndLostItemId(1L, 1L);
        verify(lostItemRepository, never()).save(any());
        verify(claimRepository, never()).save(any());
    }

    @Test
    void createClaim_ZeroQuantity() {
        // Given
        ClaimRequest zeroClaimRequest = new ClaimRequest(1L, 0, "Zero quantity");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(lostItemRepository.findById(1L)).thenReturn(Optional.of(testLostItem));
        when(claimRepository.existsByUserIdAndLostItemId(1L, 1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> claimService.createClaim(zeroClaimRequest, "testuser"))
                .isInstanceOf(InsufficientQuantityException.class);

        verify(userRepository).findByUsername("testuser");
        verify(lostItemRepository).findById(1L);
        verify(claimRepository).existsByUserIdAndLostItemId(1L, 1L);
        verify(lostItemRepository, never()).save(any());
        verify(claimRepository, never()).save(any());
    }

    @Test
    void getAllClaims_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Claim> claims = List.of(testClaim);
        Page<Claim> claimPage = new PageImpl<>(claims, pageable, 1);

        when(claimRepository.findAllWithUserAndItem(pageable)).thenReturn(claimPage);

        // When
        Page<ClaimDto> result = claimService.getAllClaims(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);

        ClaimDto claimDto = result.getContent().get(0);
        assertThat(claimDto.getId()).isEqualTo(1L);
        assertThat(claimDto.getUserId()).isEqualTo(1L);
        assertThat(claimDto.getUserName()).isEqualTo("Test User");
        assertThat(claimDto.getLostItemId()).isEqualTo(1L);
        assertThat(claimDto.getItemName()).isEqualTo("Test Laptop");
        assertThat(claimDto.getPlace()).isEqualTo("Library");
        assertThat(claimDto.getClaimedQuantity()).isEqualTo(2);
        assertThat(claimDto.getStatus()).isEqualTo(ClaimStatus.PENDING);
        assertThat(claimDto.getNotes()).isEqualTo("I need this laptop for work");

        verify(claimRepository).findAllWithUserAndItem(pageable);
    }

    @Test
    void getAllClaims_EmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Claim> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(claimRepository.findAllWithUserAndItem(pageable)).thenReturn(emptyPage);

        // When
        Page<ClaimDto> result = claimService.getAllClaims(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);

        verify(claimRepository).findAllWithUserAndItem(pageable);
    }

    @Test
    void getAllClaims_MultipleClaims() {
        // Given
        Pageable pageable = PageRequest.of(1, 5);
        
        User anotherUser = User.builder()
                .id(2L)
                .username("anotheruser")
                .name("Another User")
                .email("another@example.com")
                .role(Role.USER)
                .build();

        Claim anotherClaim = Claim.builder()
                .id(2L)
                .user(anotherUser)
                .lostItem(testLostItem)
                .claimedQuantity(1)
                .status(ClaimStatus.APPROVED)
                .notes("Different notes")
                .claimDate(LocalDateTime.now())
                .build();

        List<Claim> claims = List.of(testClaim, anotherClaim);
        Page<Claim> claimPage = new PageImpl<>(claims, pageable, 2);

        when(claimRepository.findAllWithUserAndItem(pageable)).thenReturn(claimPage);

        // When
        Page<ClaimDto> result = claimService.getAllClaims(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);

        ClaimDto firstClaimDto = result.getContent().get(0);
        assertThat(firstClaimDto.getId()).isNotNull();
        assertThat(firstClaimDto.getStatus()).isEqualTo(ClaimStatus.PENDING);

        ClaimDto secondClaimDto = result.getContent().get(1);
        assertThat(secondClaimDto.getId()).isNotNull();
        assertThat(secondClaimDto.getStatus()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(secondClaimDto.getUserName()).isEqualTo("Another User");

        verify(claimRepository).findAllWithUserAndItem(pageable);
    }
} 