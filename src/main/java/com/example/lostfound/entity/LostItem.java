package com.example.lostfound.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lost_items")
public class LostItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "item_name", nullable = false)
    private String itemName;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(name = "remaining_quantity", nullable = false)
    private Integer remainingQuantity;
    
    @Column(nullable = false)
    private String place;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    private Long version; // For optimistic locking to handle concurrency
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (remainingQuantity == null) {
            remainingQuantity = quantity;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Safely claims a quantity from this item
     * @param claimQuantity the quantity to claim
     * @return true if claim was successful, false if insufficient quantity
     */
    public boolean claimQuantity(Integer claimQuantity) {
        if (claimQuantity == null || claimQuantity <= 0) {
            return false;
        }
        
        if (remainingQuantity >= claimQuantity) {
            remainingQuantity -= claimQuantity;
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if item is available for claiming
     */
    public boolean isAvailable() {
        return remainingQuantity > 0;
    }
} 