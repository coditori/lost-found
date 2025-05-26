package com.example.lostfound.dto;

import com.example.lostfound.entity.ClaimStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDto {
    
    private Long id;
    private Long userId;
    private String userName;
    private Long lostItemId;
    private String itemName;
    private String place;
    private Integer claimedQuantity;
    private LocalDateTime claimDate;
    private ClaimStatus status;
    private String notes;
} 