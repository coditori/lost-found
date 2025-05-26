package com.example.lostfound.service.parser;

import com.example.lostfound.entity.LostItem;
import com.example.lostfound.exception.FileParsingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
public abstract class AbstractFileParsingStrategy implements FileParsingStrategy {

    @Override
    public final List<LostItem> parseFile(MultipartFile file) throws FileParsingException {
        log.info("Starting {} parsing for file: {}", getStrategyName(), file.getOriginalFilename());
        
        validateFile(file);
        
        try {
            List<LostItem> items = doParseFile(file);
            
            if (items.isEmpty()) {
                log.warn("No items parsed from file: {}", file.getOriginalFilename());
                throw new FileParsingException("No valid items found in the file");
            }
            
            log.info("Successfully parsed {} items from {}: {}", 
                    items.size(), getStrategyName(), file.getOriginalFilename());
            return items;
            
        } catch (FileParsingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse file with {}: {}", getStrategyName(), file.getOriginalFilename(), e);
            throw new FileParsingException("Error parsing file: " + e.getMessage());
        }
    }

    /**
     * Template method for specific parsing implementations
     */
    protected abstract List<LostItem> doParseFile(MultipartFile file) throws Exception;

    /**
     * Get the supported file extensions for this strategy
     */
    protected abstract List<String> getSupportedExtensions();

    /**
     * Get the supported MIME types for this strategy
     */
    protected abstract List<String> getSupportedMimeTypes();

    @Override
    public final boolean supports(String contentType, String filename) {
        // Check MIME type
        if (contentType != null && getSupportedMimeTypes().contains(contentType.toLowerCase())) {
            return true;
        }
        
        // Check file extension
        if (filename != null) {
            String lowerFilename = filename.toLowerCase();
            return getSupportedExtensions().stream()
                    .anyMatch(lowerFilename::endsWith);
        }
        
        return false;
    }

    /**
     * Common validation for uploaded files
     */
    protected void validateFile(MultipartFile file) throws FileParsingException {
        if (file == null) {
            throw new FileParsingException("File cannot be null");
        }
        
        if (file.isEmpty()) {
            throw new FileParsingException("File is empty");
        }
        
        if (file.getSize() > getMaxFileSize()) {
            throw new FileParsingException("File size exceeds maximum allowed size: " + getMaxFileSize() + " bytes");
        }
    }

    /**
     * Get maximum allowed file size in bytes (default 10MB)
     */
    protected long getMaxFileSize() {
        return 10 * 1024 * 1024; // 10MB
    }

    /**
     * Utility method to create a LostItem with common fields
     */
    protected LostItem createLostItem(String itemName, int quantity, String place) {
        return createLostItem(itemName, quantity, place, "Imported from " + getStrategyName());
    }

    /**
     * Utility method to create a LostItem with custom description
     */
    protected LostItem createLostItem(String itemName, int quantity, String place, String description) {
        return LostItem.builder()
                .itemName(itemName)
                .quantity(quantity)
                .remainingQuantity(quantity)
                .place(place)
                .description(description)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Utility method to validate and clean item data
     */
    protected void validateItemData(String itemName, Integer quantity, String place) throws FileParsingException {
        if (itemName == null || itemName.trim().isEmpty()) {
            throw new FileParsingException("Item name cannot be empty");
        }
        
        if (quantity == null || quantity <= 0) {
            throw new FileParsingException("Quantity must be a positive number");
        }
        
        if (place == null || place.trim().isEmpty()) {
            throw new FileParsingException("Place cannot be empty");
        }
    }
} 