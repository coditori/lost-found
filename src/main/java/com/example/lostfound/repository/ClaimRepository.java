package com.example.lostfound.repository;

import com.example.lostfound.entity.Claim;
import com.example.lostfound.entity.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    
    Page<Claim> findByUserId(Long userId, Pageable pageable);
    
    Page<Claim> findByLostItemId(Long lostItemId, Pageable pageable);
    
    Page<Claim> findByStatus(ClaimStatus status, Pageable pageable);
    
    @Query("SELECT c FROM Claim c JOIN FETCH c.user u JOIN FETCH c.lostItem li WHERE c.status = :status")
    List<Claim> findByStatusWithUserAndItem(@Param("status") ClaimStatus status);
    
    @Query("SELECT c FROM Claim c JOIN FETCH c.user u JOIN FETCH c.lostItem li")
    Page<Claim> findAllWithUserAndItem(Pageable pageable);
    
    @Query("SELECT SUM(c.claimedQuantity) FROM Claim c WHERE c.lostItem.id = :lostItemId AND c.status IN ('PENDING', 'APPROVED')")
    Integer getTotalClaimedQuantityForItem(@Param("lostItemId") Long lostItemId);
    
    boolean existsByUserIdAndLostItemId(Long userId, Long lostItemId);
} 