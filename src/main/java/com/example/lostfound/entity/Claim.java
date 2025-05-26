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
@Table(name = "claims")
public class Claim {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_item_id", nullable = false)
    private LostItem lostItem;
    
    @Column(name = "claimed_quantity", nullable = false)
    private Integer claimedQuantity;
    
    @Column(name = "claim_date", nullable = false)
    private LocalDateTime claimDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ClaimStatus status = ClaimStatus.PENDING;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @PrePersist
    protected void onCreate() {
        if (claimDate == null) {
            claimDate = LocalDateTime.now();
        }
    }
} 