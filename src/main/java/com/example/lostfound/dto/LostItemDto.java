package com.example.lostfound.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LostItemDto {
    
    private Long id;
    private String itemName;
    private Integer quantity;
    private Integer remainingQuantity;
    private String place;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isAvailable;
} 