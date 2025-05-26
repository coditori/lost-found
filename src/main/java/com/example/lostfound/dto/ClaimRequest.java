package com.example.lostfound.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimRequest {
    
    @NotNull(message = "Lost item ID is required")
    private Long lostItemId;
    
    @NotNull(message = "Claimed quantity is required")
    @Min(value = 1, message = "Claimed quantity must be at least 1")
    private Integer claimedQuantity;
    
    private String notes;
} 