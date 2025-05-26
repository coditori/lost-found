package com.example.lostfound.service;

import com.example.lostfound.dto.ClaimDto;
import com.example.lostfound.dto.ClaimRequest;
import com.example.lostfound.entity.Claim;
import com.example.lostfound.entity.ClaimStatus;
import com.example.lostfound.entity.LostItem;
import com.example.lostfound.entity.User;
import com.example.lostfound.exception.InsufficientQuantityException;
import com.example.lostfound.exception.LostItemNotFoundException;
import com.example.lostfound.exception.UserNotFoundException;
import com.example.lostfound.repository.ClaimRepository;
import com.example.lostfound.repository.LostItemRepository;
import com.example.lostfound.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimService {
    
    private final ClaimRepository claimRepository;
    private final LostItemRepository lostItemRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public ClaimDto createClaim(ClaimRequest request, String username) 
            throws LostItemNotFoundException, UserNotFoundException, InsufficientQuantityException {
        
        log.info("Creating claim for user: {} on item: {} with quantity: {}", 
                username, request.getLostItemId(), request.getClaimedQuantity());
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        
        LostItem lostItem = lostItemRepository.findById(request.getLostItemId())
                .orElseThrow(() -> new LostItemNotFoundException("Lost item not found with id: " + request.getLostItemId()));
        
        // Check if user has already claimed this item
        if (claimRepository.existsByUserIdAndLostItemId(user.getId(), lostItem.getId())) {
            throw new IllegalStateException("User has already claimed this item");
        }
        
        // Attempt to claim the quantity
        if (!lostItem.claimQuantity(request.getClaimedQuantity())) {
            throw new InsufficientQuantityException(
                String.format("Insufficient quantity. Requested: %d, Available: %d", 
                    request.getClaimedQuantity(), lostItem.getRemainingQuantity()));
        }
        
        // Save the updated item
        lostItemRepository.save(lostItem);
        
        // Create the claim
        Claim claim = Claim.builder()
                .user(user)
                .lostItem(lostItem)
                .claimedQuantity(request.getClaimedQuantity())
                .status(ClaimStatus.PENDING)
                .notes(request.getNotes())
                .build();
        
        Claim savedClaim = claimRepository.save(claim);
        log.info("Claim created successfully with id: {}", savedClaim.getId());
        
        return convertToDto(savedClaim);
    }
    
    public Page<ClaimDto> getAllClaims(Pageable pageable) {
        return claimRepository.findAllWithUserAndItem(pageable)
                .map(this::convertToDto);
    }
    
    private ClaimDto convertToDto(Claim claim) {
        return ClaimDto.builder()
                .id(claim.getId())
                .userId(claim.getUser().getId())
                .userName(claim.getUser().getName())
                .lostItemId(claim.getLostItem().getId())
                .itemName(claim.getLostItem().getItemName())
                .place(claim.getLostItem().getPlace())
                .claimedQuantity(claim.getClaimedQuantity())
                .claimDate(claim.getClaimDate())
                .status(claim.getStatus())
                .notes(claim.getNotes())
                .build();
    }
} 