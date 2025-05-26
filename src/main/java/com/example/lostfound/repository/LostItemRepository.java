package com.example.lostfound.repository;

import com.example.lostfound.entity.LostItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface LostItemRepository extends JpaRepository<LostItem, Long> {
    
    Page<LostItem> findByRemainingQuantityGreaterThan(Integer quantity, Pageable pageable);
    
    Page<LostItem> findByItemNameContainingIgnoreCase(String itemName, Pageable pageable);
    
    Page<LostItem> findByPlaceContainingIgnoreCase(String place, Pageable pageable);
    
    @Query("SELECT li FROM LostItem li WHERE li.remainingQuantity > 0 AND " +
           "(:itemName IS NULL OR LOWER(li.itemName) LIKE LOWER(CONCAT('%', :itemName, '%'))) AND " +
           "(:place IS NULL OR LOWER(li.place) LIKE LOWER(CONCAT('%', :place, '%')))")
    Page<LostItem> findAvailableItemsWithFilters(@Param("itemName") String itemName, 
                                                 @Param("place") String place, 
                                                 Pageable pageable);
    
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT li FROM LostItem li WHERE li.id = :id")
    Optional<LostItem> findByIdForUpdate(@Param("id") Long id);
    
    @Query("SELECT COUNT(li) FROM LostItem li WHERE li.remainingQuantity > 0")
    long countAvailableItems();
} 